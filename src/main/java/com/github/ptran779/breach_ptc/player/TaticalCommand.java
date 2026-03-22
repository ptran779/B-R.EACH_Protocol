package com.github.ptran779.breach_ptc.player;

import com.github.ptran779.breach_ptc.entity.api.EntityUtils;
import net.minecraft.nbt.CompoundTag;

public class TaticalCommand {
  private int controlFlag = 0;

  // --- Follow ---
  public boolean getFollowMode() {return (controlFlag & EntityUtils.BF_FOLLOW) != 0;}
  public void cycleFollowMode() {controlFlag ^= EntityUtils.BF_FOLLOW;}

  // --- Target ---
  public int getTargetMode() {
		boolean hostile = (controlFlag & EntityUtils.BF_TARGET_HOSTILE) != 0;
	  boolean humanoid = (controlFlag & EntityUtils.BF_TARGET_AGENT) != 0;
		if (!hostile && !humanoid) return 0;
		else if (hostile && !humanoid) return 1;
		else if (!hostile && humanoid) return 2;
		else return 3;
	}
	public void cycleTargetMode() {
		switch (getTargetMode()) {
			case 0 -> controlFlag |= EntityUtils.BF_TARGET_HOSTILE;                                                    // none → hostile
			case 1 -> { controlFlag &= ~EntityUtils.BF_TARGET_HOSTILE; controlFlag |= EntityUtils.BF_TARGET_AGENT; }  // hostile → agent
			case 2 -> controlFlag |= EntityUtils.BF_TARGET_HOSTILE;                                                    // agent → both
			case 3 -> { controlFlag &= ~EntityUtils.BF_TARGET_HOSTILE; controlFlag &= ~EntityUtils.BF_TARGET_AGENT; } // both → none
		}
	}

  // --- Special ---
  public boolean isSpecialMode() {return (controlFlag & EntityUtils.BF_ALLOW_SPECIAL) != 0;}
  public void toggleSpecialMode() {controlFlag ^= EntityUtils.BF_ALLOW_SPECIAL;}


  public void copyFrom(TaticalCommand source) {
	  controlFlag = source.controlFlag;
  }

  public void saveNBTData(CompoundTag nbt) {
    nbt.putInt("control_flag", controlFlag);
  }

  public void loadNBTData(CompoundTag nbt) {
    controlFlag = nbt.getInt("control_flag");
  }
}
