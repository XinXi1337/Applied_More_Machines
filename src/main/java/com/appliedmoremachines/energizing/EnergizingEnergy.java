package com.appliedmoremachines.energizing;
import appeng.api.config.*;
import appeng.api.networking.energy.IAEPowerStorage;
import java.util.function.DoubleSupplier;
/** Private AE-denominated buffer with a live rod-derived capacity. */
public final class EnergizingEnergy implements IAEPowerStorage {
 private double stored;
 private final DoubleSupplier capacity; private final Runnable changed;
 public EnergizingEnergy(DoubleSupplier capacity,Runnable changed){this.capacity=capacity;this.changed=changed;}
 public void clamp(){double n=Math.min(stored,getAEMaxPower());if(n!=stored){stored=n;changed.run();}}
 public void load(double n){stored=Double.isFinite(n)?Math.max(0,n):0;clamp();}
 public double injectAEPower(double amount,Actionable mode){
  if(!Double.isFinite(amount)||amount<=0)return amount;
  if(mode==Actionable.MODULATE)clamp();
  double n=Math.min(amount,Math.max(0,getAEMaxPower()-getAECurrentPower()));
  if(mode==Actionable.MODULATE&&n>0){stored+=n;changed.run();}return amount-n;
 }
 public double extractAEPower(double amount,Actionable mode,PowerMultiplier multiplier){
  if(!Double.isFinite(amount)||amount<=0||multiplier==null)return 0;
  double scaled=multiplier.multiply(amount);if(!Double.isFinite(scaled)||scaled<=0)return 0;
  if(mode==Actionable.MODULATE)clamp();
  double n=Math.min(getAECurrentPower(),scaled);
  if(mode==Actionable.MODULATE&&n>0){stored-=n;changed.run();}return multiplier.divide(n);
 }
 public double getAECurrentPower(){return Math.min(stored,getAEMaxPower());}
 public double getAEMaxPower(){double n=capacity.getAsDouble();return Double.isFinite(n)?Math.max(0,n):0;}
 public boolean isAEPublicPowerStorage(){return false;}
 public AccessRestriction getPowerFlow(){return AccessRestriction.WRITE;}
}
