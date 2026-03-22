package com.github.ptran779.breach_ptc.ai.behavior;

import com.github.ptran779.breach_ptc.ai.api.ThrottleBehavior;
import com.github.ptran779.breach_ptc.entity.agent.AbsAgentEntity;
import com.github.ptran779.breach_ptc.entity.api.EntityUtils;
import com.github.ptran779.breach_ptc.item.PatrolList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import static com.github.ptran779.breach_ptc.item.PatrolList.NBT_NAME;

public class PatrolBehavior extends ThrottleBehavior {
  protected final AbsAgentEntity agent;
	int nextIdx;
  Vec3 towards;

  public PatrolBehavior(AbsAgentEntity agent, int cooldown, int varcooldown) {
    super(agent, cooldown, varcooldown);
    this.agent = agent;
  }

	public boolean canUse() {
		return super.canUse() && !agent.isPassenger() &&
			(agent.getControlFlg1() & EntityUtils.BF_PATROL) != 0 &&
			agent.getPatrolItem().getItem() instanceof PatrolList &&
			agent.getPatrolItem().getTag() != null && // Prevent NullPointerException
			agent.getPatrolItem().getTag().contains(PatrolList.NBT_NAME, Tag.TAG_LIST);
	}

	public void start() {
		CompoundTag tag = agent.getPatrolItem().getTag();
		ListTag routeList = tag.getList(PatrolList.NBT_NAME, Tag.TAG_INT_ARRAY);

		if (routeList.isEmpty()) {
			towards = null;
			return;
		}

		// Get the size, compute the next index and loop around using Modulo
		int size = routeList.size();
		nextIdx = (agent.getLastPatrolIdx() + 1) % size;

		// Set towards position
		int[] coords = routeList.getIntArray(nextIdx);
		if (coords.length == 3) {
			// Target the center of the coordinate block
			towards = new Vec3(coords[0] + 0.5D, coords[1], coords[2] + 0.5D);
		} else {
			towards = null;
		}
	}

  public boolean run() {
    if (towards == null || (agent.getControlFlg1() & EntityUtils.BF_PATROL) == 0) {return true;}
    return !agent.moveto(towards, agent.getAttribute(Attributes.MOVEMENT_SPEED).getValue());
  }

	public void stop(){
		towards = null;
		agent.stopNav();
		agent.setLastPatrolIdx(nextIdx);
	}

	public String toString(){return "Patrol B";}
}
