package com.github.ptran779.breach_ptc.player;

//public class TaticalCommandProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
//  public static Capability<TaticalCommand> TATICAL_COMMAND_CAPABILITY = CapabilityManager.get(new CapabilityToken<TaticalCommand>() {});
//
//  private TaticalCommand taticalCommand = null;
//  private final LazyOptional<TaticalCommand> lazyOptional = LazyOptional.of(this::createTaticalCommand);
//
//  private TaticalCommand createTaticalCommand() {
//    if (taticalCommand == null) {
//      taticalCommand = new TaticalCommand();
//    }
//    return this.taticalCommand;
//  }
//
//
//  @Override
//  public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction direction) {
//    if (capability == TATICAL_COMMAND_CAPABILITY) {
//      return lazyOptional.cast();
//    }
//
//    return LazyOptional.empty();
//  }
//
//  @Override
//  public CompoundTag serializeNBT() {
//    CompoundTag tag = new CompoundTag();
//    createTaticalCommand().saveNBTData(tag);
//    return tag;
//  }
//
//  @Override
//  public void deserializeNBT(CompoundTag compoundTag) {
//    createTaticalCommand().loadNBTData(compoundTag);
//  }
//}
