package com.mouse;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.jme3.util.TempVars;
import org.dyn4j.dynamics.Body;

public class BodyControl extends AbstractControl {
    public Body body;

    public BodyControl(Body body) {
        this.body = body;
    }

    @Override
    protected void controlUpdate(float tpf) {
        TempVars temp = TempVars.get();

        float x = (float) body.getTransform().getTranslationX();
        float y = (float) body.getTransform().getTranslationY();
        float z = spatial.getLocalTranslation().z;
        spatial.setLocalTranslation(x, y, z);

        Quaternion rotation = temp.quat1;
        float angle = (float) body.getTransform().getRotationAngle();
        rotation.fromAngleNormalAxis(angle, Vector3f.UNIT_Z);
        spatial.setLocalRotation(rotation);

        temp.release();
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
}
