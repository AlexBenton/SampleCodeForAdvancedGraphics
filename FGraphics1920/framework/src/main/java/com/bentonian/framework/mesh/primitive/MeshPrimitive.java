package com.bentonian.framework.mesh.primitive;

import com.bentonian.framework.material.Material;
import com.bentonian.framework.math.Ray;
import com.bentonian.framework.math.RayIntersection;
import com.bentonian.framework.math.RayIntersections;
import com.bentonian.framework.math.Vec3;
import com.bentonian.framework.mesh.Mesh;
import com.bentonian.framework.mesh.MeshFace;
import com.bentonian.framework.mesh.MeshVertex;
import com.bentonian.framework.scene.IsRayTraceable;
import com.bentonian.framework.ui.GLCanvas;
import com.bentonian.framework.ui.GLVertexData;

public class MeshPrimitive extends CompiledPrimitive implements IsRayTraceable {

  public enum RenderStyle {
    NORMALS_BY_FACE,
    NORMALS_BY_VERTEX
  }

  private final Mesh mesh;

  private MeshPrimitiveFeatureAccelerator featuresAccelerator;
  private MeshPrimitiveRayTracingAccelerator rayTracingAccelerator;
  private RenderStyle renderStyle = RenderStyle.NORMALS_BY_FACE;

  public MeshPrimitive(Mesh mesh) {
    super(GLVertexData.Mode.TRIANGLES);
    this.mesh = mesh;
  }

  public Mesh getMesh() {
    return mesh;
  }

  public void setRenderStyle(RenderStyle renderStyle) {
    if (this.renderStyle != renderStyle) {
      this.renderStyle = renderStyle;
      dispose();
    }
  }

  public RenderStyle getRenderStyle() {
    return renderStyle;
  }

  public void enableRayTracingAccelerator() {
    rayTracingAccelerator = new MeshPrimitiveRayTracingAccelerator(mesh);
  }

  public void disableRayTracingAccelerator() {
    rayTracingAccelerator = null;
  }

  public MeshPrimitiveFeatureAccelerator getFeaturesAccelerator() {
    if (featuresAccelerator == null) {
      featuresAccelerator = new MeshPrimitiveFeatureAccelerator(mesh);
    }
    return featuresAccelerator;
  }

  @Override
  public RayIntersections traceLocal(Ray ray) {
    RayIntersections hits = new RayIntersections();

    if (rayTracingAccelerator == null) {
      for (MeshFace face : mesh) {
        traceRayToFace(ray, hits, face);
      }
    } else if (rayTracingAccelerator.isHitByRay(ray)) {
      for (MeshFace face : rayTracingAccelerator.getFacesAlongRay(ray)) {
        traceRayToFace(ray, hits, face);
      }
    }
    return hits;
  }

  public void traceRayToFace(Ray ray, RayIntersections hits, MeshFace face) {
    for (int i = 0; i < face.size() - 2; i++) {
      MeshVertex A = face.get(0);
      MeshVertex B = face.get(i + 1);
      MeshVertex C = face.get(i + 2);
      Double t = ray.intersectsTriangle(A, B, C, face.getNormal());
      if (t != null) {
        Vec3 pt = ray.at(t);
        Material material = getMaterial(pt);
        Vec3 normal = (renderStyle == RenderStyle.NORMALS_BY_FACE)
            ? face.getNormal() : getNormalFromBarycentricWeights(A, B, C, pt);
        hits.add(new RayIntersection(this, t, pt, normal, material));
      }
    }
  }

  private Vec3 getNormalFromBarycentricWeights(MeshVertex A, MeshVertex B, MeshVertex C, Vec3 pt) {
    double barycentricWeightA = B.minus(pt).cross(C.minus(pt)).length();
    double barycentricWeightB = C.minus(pt).cross(A.minus(pt)).length();
    double barycentricWeightC = A.minus(pt).cross(B.minus(pt)).length();
    return A.getNormal().times(barycentricWeightA)
        .plus(B.getNormal().times(barycentricWeightB))
        .plus(C.getNormal().times(barycentricWeightC))
        .normalized();
  }

  protected Material getMaterial(Vec3 pt) {
    return getMaterial();
  }

  @Override
  protected void renderLocal(GLCanvas glCanvas) {
    if (featuresAccelerator != null) {
      featuresAccelerator.render(glCanvas);
    }
    if (!isCompiled()) {
      renderFaces();
    }
    super.renderLocal(glCanvas);
  }

  protected void renderFaces() {
    for (MeshFace face : mesh) {
      renderFace(face);
    }
  }

  @Override
  public void dispose() {
    if (featuresAccelerator != null) {
      featuresAccelerator.dispose();
    }
    super.dispose();
    // TODO(me) Reset raytracing accelerator here?
  }

  protected void renderFace(MeshFace face) {
    if (renderStyle == RenderStyle.NORMALS_BY_FACE) {
      normal(face.getNormal());
    }
    for (int i = 0; i < face.size() - 2; i++) {
      renderVertex(face, 0);
      renderVertex(face, i + 1);
      renderVertex(face, i + 2);
    }
  }

  protected void renderVertex(MeshFace face, int index) {
    MeshVertex v = face.get(index);
    if (renderStyle == RenderStyle.NORMALS_BY_VERTEX) {
      normal(v.getNormal());
    }
    color(getMaterial(v).getColor());
    vertex(v);
  }
}
