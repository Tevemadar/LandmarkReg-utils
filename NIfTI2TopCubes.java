import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import nii.Nifti1Dataset;
import png.PngWriter;

public class NIfTI2TopCubes {
	public static void main(String[] args) throws Exception {
		String name=args[0];
		Nifti1Dataset nii=new Nifti1Dataset(name);
		name=name.substring(0, name.indexOf('.'));
		nii.readHeader();
		short datatype=nii.datatype;
		int bpv=Nifti1Dataset.bytesPerVoxel(datatype);
		if(nii.TDIM!=0 && nii.TDIM!=1)
			throw new Exception("TDIM="+nii.TDIM);
		int channels=1;
		for(int i=5;i<nii.dim.length;i++)
			channels*=Math.max(1, nii.dim[i]);
		int xdim=nii.XDIM;
		int ydim=nii.YDIM;
		int zdim=nii.ZDIM;
		long blobsize=(long)xdim*ydim*zdim*bpv;
		try(DataInputStream dis=new DataInputStream(nii.ds_datname.endsWith(".gz")?new GZIPInputStream(new FileInputStream(nii.ds_datname)):new FileInputStream(nii.ds_datname));
				PrintWriter json=new PrintWriter(name+".json")){
			head(json,name,xdim,ydim,zdim);
			dis.skipBytes((int)nii.vox_offset);
			for(int channel=0;channel<channels;channel++) {
				String folder=name+"_c"+channel;
				json.print("        {\r\n" + 
				"            \"name\": \"Channel #"+channel+"\",\r\n" + 
				"            \"type\": \"gray8\",\r\n" + 
				"            \"urlformatter\": \"'"+folder+"/'+l+'/'+z+'/'+y+'/'+x+'.png'\"\r\n" + 
				"        }");
				if(channel!=channels-1)
					json.print(',');
				json.print("\r\n"); 
				ArrayList<byte[]> blobs=new ArrayList<>();
				long size=blobsize;
				while(size>0){
					byte blob[]=new byte[(int)Math.min(size, 1024*1024*1024)];
					dis.readFully(blob);
					blobs.add(blob);
					size-=blob.length;
				}
				ByteBuffer bbs[]=new ByteBuffer[blobs.size()];
				for(int i=0;i<bbs.length;i++){
					ByteBuffer bb=ByteBuffer.wrap(blobs.get(i));
					bb.order(nii.big_endian?ByteOrder.BIG_ENDIAN:ByteOrder.LITTLE_ENDIAN);
					bbs[i]=bb;
				}
				double min=Float.MAX_VALUE;
				double max=-Float.MAX_VALUE;
				for(int i=0;i<bbs.length;i++){
					ByteBuffer bb=bbs[i];
					while(bb.position()<bb.limit()){
						double d=0;
						switch(datatype) {
						case Nifti1Dataset.NIFTI_TYPE_FLOAT32:
							d=bb.getFloat();
							break;
						case Nifti1Dataset.NIFTI_TYPE_FLOAT64:
							d=bb.getDouble();
							break;
						case Nifti1Dataset.NIFTI_TYPE_INT8:
							d=bb.get();
							break;
						case Nifti1Dataset.NIFTI_TYPE_UINT8:
							d=bb.get() & 255;
							break;
						default:
							throw new Exception(Nifti1Dataset.decodeDatatype(datatype)+" is not implemented.");
						}
						if(d<min)min=d;
						if(d>max)max=d;
					}
				}
				System.out.println(min+"-"+max);
				final int xcubes=(int)Math.ceil(xdim/64.0);
				final int ycubes=(int)Math.ceil(ydim/64.0);
				final int zcubes=(int)Math.ceil(zdim/64.0);
				final String dirformat=folder+"\\0\\%d\\%d";
				final String pngformat=dirformat+"\\%d.png";
				
				final byte cube[]=new byte[64*64*64];
				final byte line[]=new byte[4096];
				for(int y=0;y<ycubes;y++){
					for(int z=0;z<zcubes;z++){
						new File(String.format(dirformat, y, z)).mkdirs();
						for(int x=0;x<xcubes;x++){
							Arrays.fill(cube, (byte)0);
							for(int yy=0;(yy<64) && (y*64+yy<ydim);yy++)
								for(int zz=0;(zz<64) && (z*64+zz<zdim);zz++)
									for(int xx=0;(xx<64) && (x*64+xx<xdim);xx++){
//										long tx=x*64+xx;
//										long ty=y*64+yy;
//										long tz=z*64+zz;
										long longpos=((x*64+xx)+(y*64+yy)*xdim+(zdim-1-(z*64+zz))*(long)xdim*ydim)*bpv;
										int blb=(int)(longpos & 0x3FFFFFFF);
										int idx=(int)(longpos >> 30);
										ByteBuffer bb=bbs[idx];
										double d=0;
										switch(datatype) {
										case Nifti1Dataset.NIFTI_TYPE_FLOAT32:
											d=bb.getFloat(blb);
											break;
										case Nifti1Dataset.NIFTI_TYPE_FLOAT64:
											d=bb.getDouble(blb);
											break;
										case Nifti1Dataset.NIFTI_TYPE_INT8:
											d=bb.get(blb);
											break;
										case Nifti1Dataset.NIFTI_TYPE_UINT8:
											d=bb.get(blb) & 255;
											break;
										default:
											throw new Exception(Nifti1Dataset.decodeDatatype(datatype)+" is not implemented.");
										}
										cube[xx+zz*64+yy*64*64]=(byte)((d-min)*255/(max-min));
										//cube[xx+zz*64+yy*64*64]=(byte)(bbs[idx].getFloat(blb)*255/max);
									}
							final BufferedOutputStream bos=new BufferedOutputStream(new FileOutputStream(String.format(pngformat, y,z,x)));
							final PngWriter png=new PngWriter(bos, 4096, 64, PngWriter.TYPE_GRAYSCALE, null);
							for(int zz=0;zz<64;zz++){
								System.arraycopy(cube, zz*4096, line, 0, 4096);
								png.writeline(line);
							}
							bos.close();
						}
					}
				}
				CuPyd.main(new String[] {
						Integer.toString(xdim),
						Integer.toString(ydim),
						Integer.toString(zdim),
						folder
						});
			}
			tail(json);
		}
	}
	static void head(PrintWriter json,String name,int xdim,int ydim,int zdim) {
		double xcubes=Math.ceil(xdim/64.0);
		double ycubes=Math.ceil(ydim/64.0);
		double zcubes=Math.ceil(zdim/64.0);
		int maxlevel=0;
		while(xcubes*ycubes*zcubes>1) {
			maxlevel++;
			xcubes=Math.ceil(xcubes/2);
			ycubes=Math.ceil(ycubes/2);
			zcubes=Math.ceil(zcubes/2);
		}
		json.print("{\r\n" + 
				"    \"name\": \""+name+"\",\r\n" + 
				"\r\n" + 
				"    \"volume\":{\r\n" + 
				"        \"width\": "+xdim+",\r\n" + 
				"        \"height\": "+zdim+",\r\n" + 
				"        \"length\": "+ydim+"\r\n" + 
				"    },\r\n" + 
				"\r\n" + 
				"    \"tech\":{\r\n" + 
				"        \"xdim\": "+xdim+",\r\n" + 
				"        \"zdim\": "+ydim+",\r\n" + 
				"        \"ydim\": "+zdim+",\r\n" +
				"        \"maxlevel\": "+maxlevel+"\r\n" + 
				"    },\r\n" + 
				"    \r\n" + 
				"    \"basetrf\": [\r\n" + 
				"        [1,0,0,0],\r\n" + 
				"        [0,1,0,0],\r\n" + 
				"        [0,0,1,0],\r\n" + 
				"        [0,0,0,1]\r\n" + 
				"    ],\r\n" + 
				"    \r\n" + 
				"    \"mods\":[\r\n");
	}
	static void tail(PrintWriter json) {
		json.print("    ]\r\n" + 
				"}\r\n");
	}
}
