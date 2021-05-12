import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
//import java.util.concurrent.Executor;
//import java.util.concurrent.ExecutorCompletionService;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import png.PngWriter;

public class CuPyd {
//	static final int ydim=528;//1320;
//	static final int zdim=320;//800;
//	static final int xdim=456;//1140;
//	public static final int xdim=1823;
//	public static final int zdim=1351;
//	public static final int ydim=2697;
	public static void main(String[] args) throws Exception {
//		ExecutorService es=Executors.newFixedThreadPool(64);
//		ExecutorCompletionService<Object> ecs=new ExecutorCompletionService<>(es);
//		final int ydim=1320;
//		final int zdim=800;
//		final int xdim=1140;
//		final int xdim=512;
//		final int ydim=1024;
//		final int zdim=512;
//		final int xdim=1228;
//		final int ydim=2016;
//		final int zdim=830;
		final int xdim=Integer.parseInt(args[0]);
		final int ydim=Integer.parseInt(args[1]);
		final int zdim=Integer.parseInt(args[2]);
//		final String dirformat="%02d\\%03d\\%03d";
//		final String pngformat=dirformat+"\\%03d.png";
		final String dirformat=args.length==4?args[3]+"\\%d\\%d\\%d":"%d\\%d\\%d";
		final String pngformat=dirformat+"\\%d.png";
		final int cube[]=new int[128*128*128];
//		byte flat[]=new byte[512*512];
		byte flat[]=new byte[4096*64];
//		final byte line[]=new byte[512];
		final byte line[]=new byte[4096];
		
		int prevxcubes=(int)Math.ceil(xdim/64.0);
		int prevycubes=(int)Math.ceil(ydim/64.0);
		int prevzcubes=(int)Math.ceil(zdim/64.0);
		int level=0;
		do{
			int xcubes=(int)Math.ceil(prevxcubes/2.0);
			int ycubes=(int)Math.ceil(prevycubes/2.0);
			int zcubes=(int)Math.ceil(prevzcubes/2.0);
			level++;
			for(int z=0;z<zcubes;z++)
				for(int y=0;y<ycubes;y++) {
					System.out.println(String.format("%02d (%d), %02d (%d)", z,zcubes,y,ycubes));
					new File(String.format(dirformat,level,y,z)).mkdirs();
					for(int x=0;x<xcubes;x++) {
						Arrays.fill(cube, 0);
						for(int i=0;i<8;i++) {
							int dx=i & 1;
							int dy=(i >> 1) & 1;
							int dz=(i >> 2) & 1;
							if(x*2+dx<prevxcubes && y*2+dy<prevycubes && z*2+dz<prevzcubes) {
//								flat=(byte[])ImageIO.read(new File(String.format(pngformat,level-1,y*2+dy,z*2+dz,x*2+dx))).getRaster().getDataElements(0, 0, 512, 512, flat);
								flat=(byte[])ImageIO.read(new File(String.format(pngformat,level-1,y*2+dy,z*2+dz,x*2+dx))).getRaster().getDataElements(0, 0, 4096, 64, flat);
								dx*=64;
								dy*=64;
								dz*=64;
								for(int yy=0;yy<64;yy++)
									for(int zz=0;zz<64;zz++)
										for(int xx=0;xx<64;xx++)
											cube[xx+dx+(zz+dz)*128+(yy+dy)*128*128]=flat[xx+zz*64+yy*64*64] & 255;
							}
						}
						for(int yy=0;yy<64;yy++)
							for(int zz=0;zz<64;zz++)
								for(int xx=0;xx<64;xx++)
									flat[xx+zz*64+yy*64*64]=(byte)((
											cube[xx*2+  zz*2*128+    yy*2*128*128]+
											cube[xx*2+1+zz*2*128+    yy*2*128*128]+
											cube[xx*2+  zz*2*128+128+yy*2*128*128]+
											cube[xx*2+1+zz*2*128+128+yy*2*128*128]+
											cube[xx*2+  zz*2*128+    yy*2*128*128+128*128]+
											cube[xx*2+1+zz*2*128+    yy*2*128*128+128*128]+
											cube[xx*2+  zz*2*128+128+yy*2*128*128+128*128]+
											cube[xx*2+1+zz*2*128+128+yy*2*128*128+128*128]
											)/8);
						final BufferedOutputStream bos=new BufferedOutputStream(new FileOutputStream(String.format(pngformat,level,y,z,x)));
//						final PngWriter png=new PngWriter(bos, 512, 512, PngWriter.TYPE_GRAYSCALE, null);
//						for(int zz=0;zz<512;zz++){
//							System.arraycopy(flat, zz*512, line, 0, 512);
//							png.writeline(line);
//						}
						final PngWriter png=new PngWriter(bos, 4096, 64, PngWriter.TYPE_GRAYSCALE, null);
						for(int zz=0;zz<64;zz++){
							System.arraycopy(flat, zz*4096, line, 0, 4096);
							png.writeline(line);
						}
						bos.close();
					}
				}
			prevxcubes=xcubes;
			prevycubes=ycubes;
			prevzcubes=zcubes;
		}while(prevxcubes*prevycubes*prevzcubes>1);
	}
}
