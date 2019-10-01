package com.bentonian.gldemos.gpurender;

import static com.bentonian.framework.ui.ShaderUtil.loadShader;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.filechooser.FileSystemView;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL20;

import com.bentonian.framework.io.FileUtil;
import com.bentonian.framework.math.Vec3;
import com.bentonian.framework.mesh.primitive.Square;
import com.bentonian.framework.scene.Camera;
import com.bentonian.framework.texture.BufferedImageTexture;
import com.bentonian.framework.ui.BufferedImageRGBCanvas;
import com.bentonian.framework.ui.DemoApp;
import com.bentonian.framework.ui.ShaderAutoloader;

public class GPURenderDemo extends DemoApp {

  private static final String[] SHADERS = { 
    "dancing cubes.fsh", 
    "interpolation.fsh",
    "affine transforms.fsh",
    "blending.fsh",
    "bending.fsh",
    "ambient occlusion.fsh",
    "repetition.fsh",
    "repetition2.fsh",
//    "voronoi cells.fsh",
    "reflection and refraction.fsh", 
    "primitives.fsh", 
//    "noise.fsh",
    "raytracing.fsh", 
//    "bezier.fsh",
    "simple metaballs.fsh", 
    "lecture demo.fsh",
  };

  private final Square square;

  private Camera frozenCamera;
  private boolean showRenderDepth;
  private int currentShader;
  private long elapsed, lastTick;
  private boolean paused;
  
  private boolean record;
  private List<BufferedImage> frames;
  
  private ShaderAutoloader loader;

  protected GPURenderDemo() {
    super("GPU Render");
    this.square = new Square();
    this.square.setHasTexture(true);
    this.currentShader = 0;
    this.paused = false;
    this.record = false;
    this.elapsed = 0;
    this.lastTick = System.currentTimeMillis();

    setCameraDistance(2.15);
    this.frozenCamera = new Camera(getCamera());
    animateCameraToPosition(new Vec3(20, 10, 20));

    // This isn't actually textured onto the quad, but it's a handy way to pass the image
    this.square.setTexture(new BufferedImageTexture(GPURenderDemo.class, "background.jpg"));

    this.loader = new ShaderAutoloader(
        new String[] { getRoot(), getRoot() + "include/" },
        () -> loadShader(GL20.GL_VERTEX_SHADER, getRoot() + "include/basic.vsh"),
        () -> loadShader(GL20.GL_FRAGMENT_SHADER, getRoot() + SHADERS[currentShader]),
        () -> exitRequested,
        (p) -> useProgram(p),
        (e) -> System.err.println(e)
    );
  }

  @Override
  protected Camera getCameraForModelview() {
    return frozenCamera;
  }

  @Override
  public void preDraw() {
    loader.preDraw();
    super.preDraw();
  }

  @Override
  public void draw() {
    Vec3 camPos = getCamera().getPosition();
    Vec3 camDir = getCamera().getDirection();
    Vec3 camUp = getCamera().getUp();
    long now = System.currentTimeMillis();

    if (!paused) {
      elapsed += now - lastTick;
    }
    lastTick = now;
    updateUniformVec2("iResolution", (float) getWidth(), (float) getHeight());
    updateUniformFloat("iGlobalTime", getIGlobalTime());
    updateUniformVec3("iRayOrigin", camPos);
    updateUniformVec3("iRayDir", camDir);
    updateUniformVec3("iRayUp", camUp);
    square.render(this);
  }
  
  private float getIGlobalTime() {
    return record 
        ? (frames.size() / 33.0f)
        : (elapsed / 1000.0f);
  }
  
  @Override
  public void postDraw() {
    if (record) {
      frames.add(BufferedImageRGBCanvas.copyOpenGlContextToImage(width, height, width, height));      
      if (frames.size() >= 33 * 5 /* 33 fps for five seconds */) {
        String filename = FileSystemView.getFileSystemView().getHomeDirectory() + "\\" + SHADERS[currentShader].replace(".fsh",  "");
        FileUtil.writeGif(frames, filename);
        record = false;
        System.out.println("Recording stopped");
      }
    }
    super.postDraw();
  }

  @Override
  protected void onResized(int width, int height) {
    super.onResized(width, height);
    square.setIdentity();
    square.scale(new Vec3(width / (float) height, 1, 1));
    if (getProgram() != -1) {
      updateUniformVec2("iResolution", (float) width, (float) height);
    }
  }

  @Override
  public void onKeyDown(int key) {
    switch (key) {
    case GLFW.GLFW_KEY_R:
      if (isControlDown()) {
        record = !record;
        if (record) {
          System.out.println("Recording started");
          frames = new ArrayList<>();
        } else {
          System.out.println("Recording stopped");
        }
      } else {
        showRenderDepth = !showRenderDepth;
        updateUniformBoolean("iShowRenderDepth", showRenderDepth);
      }
      break;
    case GLFW.GLFW_KEY_4:
      animateCameraToPosition(new Vec3(2, 1, 2).normalized().times(8));
      break;
    case GLFW.GLFW_KEY_J:
      elapsed -= Math.PI * 1000.0 / 16.0;
      break;
    case GLFW.GLFW_KEY_K:
      elapsed += Math.PI * 1000.0 / 16.0;
      break;
    case GLFW.GLFW_KEY_LEFT_BRACKET:
      currentShader = (currentShader + SHADERS.length - 1) % SHADERS.length;
      loader.updateProgram();
      break;
    case GLFW.GLFW_KEY_RIGHT_BRACKET:
      currentShader = (currentShader + 1) % SHADERS.length;
      loader.updateProgram();
      break;
    case GLFW.GLFW_KEY_SPACE:
      paused = !paused;
      break;
    default: super.onKeyDown(key);
      break;
    }
  }

  private String getRoot() {
    return GPURenderDemo.class.getPackage().getName().replace(".", "/") + "/";
  }

  /////////////////////////////////////////////////////////////////////////////

  public static void main(String[] args) {
    new GPURenderDemo().run();
  }
}
