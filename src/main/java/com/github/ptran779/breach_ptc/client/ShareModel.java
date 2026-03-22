package com.github.ptran779.breach_ptc.client;

import com.github.ptran779.breach_ptc.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShareModel {
  /// Use to share complex model if possible -- so BE, E, Item, literally anytype can share the model without needing to own a seperate thing
  public static HellpodModel HELLPOD_MODEL_SHARE;
  public static GrenadeModel GRENADE_MODEL_SHARE;
	public static KSeedCoreModel K_SEED_CORE_MODEL_SHARE;
	public static VoidDrifterModuleModel VOID_DRIFTER_MODULE_MODEL_SHARE;
	public static AgentModel A_THICK_MODEL_SHARE, A_SLIM_MODEL_SHARE;

  public static HellpodModel prepHellPodModel(ModelPart part) {
    if (HELLPOD_MODEL_SHARE == null) {HELLPOD_MODEL_SHARE = new HellpodModel(part);}
    return HELLPOD_MODEL_SHARE;
  }

	public static KSeedCoreModel prepKSeedCoreModel(ModelPart part) {
		if (K_SEED_CORE_MODEL_SHARE == null) {K_SEED_CORE_MODEL_SHARE = new KSeedCoreModel(part);}
		return K_SEED_CORE_MODEL_SHARE;
	}

	public static VoidDrifterModuleModel prepVoidDrifterModuleModel(ModelPart part) {
		if (VOID_DRIFTER_MODULE_MODEL_SHARE == null) {VOID_DRIFTER_MODULE_MODEL_SHARE = new VoidDrifterModuleModel(part);}
		return VOID_DRIFTER_MODULE_MODEL_SHARE;
	}

  public static GrenadeModel prepGrenadeModel(ModelPart part) {
    if (GRENADE_MODEL_SHARE == null) {GRENADE_MODEL_SHARE = new GrenadeModel(part);}
    return GRENADE_MODEL_SHARE;
  }

	public static AgentModel prepAgentThickModel(ModelPart part) {
		if (A_THICK_MODEL_SHARE == null) {
			A_THICK_MODEL_SHARE = new AgentModel(part, false);
			A_THICK_MODEL_SHARE.young = false;
		}
		return A_THICK_MODEL_SHARE;
	}

	public static AgentModel prepAgentSlimModel(ModelPart part) {
		if (A_SLIM_MODEL_SHARE == null) {
			A_SLIM_MODEL_SHARE = new AgentModel(part, true);
			A_SLIM_MODEL_SHARE.young = false;
		}
		return A_SLIM_MODEL_SHARE;
	}
}
