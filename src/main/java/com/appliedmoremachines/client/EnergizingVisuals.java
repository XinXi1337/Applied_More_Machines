package com.appliedmoremachines.client;
import net.minecraft.client.gui.GuiGraphics;
public final class EnergizingVisuals {
 private static final String[] GLYPHS={"01110/11011/11011/11111/11011/11011/11011","11111/11000/11000/11110/11000/11000/11111"};
 public static void progress(GuiGraphics g,int x,int y,double progress){
  int width=(int)(28*Math.clamp(progress,0,1));
  for(int n=0;n<2;n++){String[] rows=GLYPHS[n].split("/");for(int r=0;r<7;r++)for(int c=0;c<5;c++)if(rows[r].charAt(c)=='1'){
   int px=(n==0?2:16)+c*2;if(px>=width)continue;g.fill(x+px,y+3+r*2,x+Math.min(px+2,width),y+5+r*2,0xff946bc4);
  }}
 }
 private EnergizingVisuals(){}
}
