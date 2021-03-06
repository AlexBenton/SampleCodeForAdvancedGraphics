package com.bentonian.gldemos.shaders;

import java.awt.Point;

import org.lwjgl.glfw.GLFW;

import com.bentonian.framework.math.Vec3;
import com.bentonian.framework.math.M4x4;
import com.bentonian.framework.mesh.primitive.MeshPrimitive;
import com.bentonian.framework.ui.DemoApp;

public class ShaderDemo extends DemoApp {

  static private final double CAMERA_DISTANCE = 5.0;

  private static final ShaderRenderer[] shaders = {
    new ShaderRenderer("basic.vsh", "basic.fsh"),
    new ShaderRenderer("gouraud.vsh", "gouraud.fsh"),
    new ShaderRenderer("phong.vsh", "phong.fsh"),
    new ShaderRenderer("procedural.vsh", "procedural.fsh"),
    new GoochRenderer(),
    new ShaderRenderer("lattice.vsh", "lattice.fsh"),
    new ShaderRenderer("quilez-voronoi.vsh", "quilez-voronoi.fsh"),
    new PingRenderer(),
    new MandelbrotRenderer(),
  };

  private ShaderModel model;
  private int currentShader = -1;
  private int nextShader = -1;
  private float mandelbrotZoom = 1;
  private float mandelbrotCenterX = 0;
  private float mandelbrotCenterY = 0;
  private boolean spin = false;
  private Vec3 pingPoint;

  public ShaderDemo() {
    super("Shader demo");
    this.height = this.width;
    this.nextShader = 0;
    this.model = ShaderModel.CUBE;
    setCameraDistance(CAMERA_DISTANCE);
  }

  @Override
  public String getTitle() {
    return "Shader Demo";
  }

  @Override
  public void onKeyDown(int key) {
    switch (key) {
    case GLFW.GLFW_KEY_SPACE:
      spin = !spin;
      break;
    case GLFW.GLFW_KEY_1:
    case GLFW.GLFW_KEY_2:
    case GLFW.GLFW_KEY_3:
      super.onKeyDown(key);
      mandelbrotZoom = 1;
      mandelbrotCenterX = 0;
      mandelbrotCenterY = 0;
      break;
    case GLFW.GLFW_KEY_0:
    {
      Vec3 pt = new Vec3(1, 1, 1).normalized().times(CAMERA_DISTANCE);
      pt = M4x4.rotationMatrix(new Vec3(0, 1, 0), 0.15).times(pt);
      pt = M4x4.rotationMatrix(getCamera().getLocalToParent().extract3x3().times(new Vec3(1, 0, 0)), -0.15).times(pt);
      animateCameraToPosition(pt);
      break;
    }
    case GLFW.GLFW_KEY_MINUS:
      nextShader = (currentShader + shaders.length - 1) % shaders.length;
      break;
    case GLFW.GLFW_KEY_EQUAL:
      nextShader = (currentShader + shaders.length + 1) % shaders.length;
      break;
    case GLFW.GLFW_KEY_E:
      for (ShaderModel shadeable : ShaderModel.values()) {
        if (shadeable.getGeometry() instanceof MeshPrimitive) {
          MeshPrimitive meshPrimitive = (MeshPrimitive) shadeable.getGeometry();
          meshPrimitive.getFeaturesAccelerator().setShowEdges(
              !meshPrimitive.getFeaturesAccelerator().getShowEdges());
        }
      }
      break;
    case GLFW.GLFW_KEY_N:
      for (ShaderModel shadeable : ShaderModel.values()) {
        if (shadeable.getGeometry() instanceof MeshPrimitive) {
          MeshPrimitive meshPrimitive = (MeshPrimitive) shadeable.getGeometry();
          meshPrimitive.getFeaturesAccelerator().setShowNormals(
              !meshPrimitive.getFeaturesAccelerator().getShowNormals());
        }
      }
      break;
    case GLFW.GLFW_KEY_LEFT_BRACKET:
      model = model.prev();
      model.dispose();
      break;
    case GLFW.GLFW_KEY_RIGHT_BRACKET:
      model = model.next();
      model.dispose();
      break;
    default: super.onKeyDown(key);
      break;
    }
  }

  @Override
  public void onMouseMove(int x, int y) {
    super.onMouseMove(x, y);
    if ((currentShader != -1) 
        && shaders[currentShader].getClass().getName().contains("Ping")) {
      pingPoint = pickPoint(model.getGeometry(), x, y);
    }
  }

  @Override
  protected void draw() {
    if (spin) {
      getCamera().rotate(getCamera().getLocalToParent().extract3x3().times(new Vec3(0, 1, 0)), 0.01);
    }
    
    if (nextShader != -1) {
      if (currentShader != -1) {
        shaders[currentShader].disable(this);
      }
      currentShader = nextShader;
      model.dispose();
      shaders[currentShader].init(this);
      setTitle("Shader Demo - " + shaders[currentShader].getName());
      nextShader = -1;
    }

    shaders[currentShader].render(this, model);
  }

  @Override
  public void onMouseDrag(int x, int y) {
    if (isControlDown() && lastCapturedMousePosition != null) {
      int dx = x - lastCapturedMousePosition.x;
      int dy = y - lastCapturedMousePosition.y;
      lastCapturedMousePosition = new Point(x, y);
      mandelbrotCenterX -= dx / (10 * mandelbrotZoom);
      mandelbrotCenterY += dy / (10 * mandelbrotZoom);
    } else {
      super.onMouseDrag(x, y);
    }
  }

  @Override
  public void onMouseWheel(int delta) {
    if (isControlDown()) {
      double notches = -delta / 50.0;
      mandelbrotZoom *= Math.pow(1.1, notches);
    } else {
      super.onMouseWheel(delta);
    }
  }

  float getMandelbrotZoom() {
    return mandelbrotZoom;
  }

  float getMandelbrotCenterX() {
    return mandelbrotCenterX;
  }

  float getMandelbrotCenterY() {
    return mandelbrotCenterY;
  }

  Vec3 getPingPoint() {
    return pingPoint;
  }

  public static void main(String[] args) {
    new ShaderDemo().run();
  }
}
