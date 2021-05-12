import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import nii.Nifti1Dataset;
import parsers.JSON;

public class PituBuild {
	public static void main(String[] args) throws Exception {
		Nifti1Dataset target=new Nifti1Dataset(args[0]);
		target.readHeader();
		Nifti1Dataset source=new Nifti1Dataset(args[1]);
		source.readHeader();
		if(source.datatype!=Nifti1Dataset.NIFTI_TYPE_UINT8 || target.datatype!=Nifti1Dataset.NIFTI_TYPE_UINT8)
			throw new Exception("Non-UINT8");

		byte[][]trg=new byte[target.dim[5]][];
		try(FileInputStream fis=new FileInputStream(target.ds_datname)){
			fis.skipNBytes((long)target.vox_offset);
			for(int i=0;i<trg.length;i++)
				trg[i]=fis.readNBytes(target.XDIM*target.YDIM*target.ZDIM);
			if(fis.available()>0)throw new Exception();
		}
		byte[][]src=new byte[source.dim[5]][];
		try(FileInputStream fis=new FileInputStream(source.ds_datname)){
			fis.skipNBytes((long)source.vox_offset);
			for(int i=0;i<src.length;i++)
				src[i]=fis.readNBytes(source.XDIM*source.YDIM*source.ZDIM);
			if(fis.available()>0)throw new Exception();
		}
		
		Nifti1Dataset result=new Nifti1Dataset();
		result.copyHeader(target);
		result.dim[3]=result.ZDIM+=source.ZDIM;
		result.setHeaderFilename(args[3]);
		result.setDataFilename(args[3]);
		result.writeHeader();
		
		Object o=JSON.parse(new FileReader(args[2]));
		List<Landmark> landmarks=new ArrayList<>();
		JSON.mapList(o, landmarks, Landmark.class, null);
		float[][]A=new float[landmarks.size()][4];
		float[][]B=new float[landmarks.size()][4];
		int i=0;
		for(Landmark l:landmarks) {
			A[i][0]=(float)l.x;
			A[i][1]=(float)l.y;
			A[i][2]=(float)l.z;
			A[i][3]=1;
			B[i][0]=(float)l.px;
			B[i][1]=(float)l.py;
			B[i][2]=(float)l.pz;
			B[i][3]=1;
			i++;
		}
        float AT[][]=Mtx.tpose(A);
        float ATA[][]=Mtx.mult(AT, A);
        float inv[][]=Mtx.inv4x4(ATA);
        float trf[][]=Mtx.mult(Mtx.mult(inv, AT), B);
		
		byte[][]res=new byte[trg.length][result.XDIM*result.YDIM*result.ZDIM];
		for(i=0;i<trg.length;i++)
			for(int z=0;z<result.ZDIM;z++)
				for(int y=0;y<result.YDIM;y++)
					for(int x=0;x<result.XDIM;x++) {
						int trgsample=0;
						int ltx=x,
							lty=target.ZDIM-z,
							ltz=y;
						int tx=ltx,
							ty=ltz,
							tz=target.ZDIM-lty;
						if(tx>=0 && tx<target.XDIM && ty>=0 && ty<target.YDIM && tz>=0 && tz<target.ZDIM)
							trgsample=trg[i][tx+ty*target.XDIM+tz*target.XDIM*target.YDIM] & 255;
		            	int lsx=(int)(ltx*trf[0][0]+lty*trf[1][0]+ltz*trf[2][0]+trf[3][0]);
		            	int lsy=(int)(ltx*trf[0][1]+lty*trf[1][1]+ltz*trf[2][1]+trf[3][1]);
		            	int lsz=(int)(ltx*trf[0][2]+lty*trf[1][2]+ltz*trf[2][2]+trf[3][2]);
						int srcsample=0;
						int sx=lsx,
							sy=lsz,
							sz=source.ZDIM-lsy;
						if(sx>=0 && sx<source.XDIM && sy>=0 && sy<source.YDIM && sz>=0 && sz<source.ZDIM)
							srcsample=src[i][sx+sy*source.XDIM+sz*source.XDIM*source.YDIM] & 255;
						byte sample=(byte)Math.max(trgsample, srcsample);
//						if(0<=ltx && ltx<10)sample=64;
//						if(0<=lty && lty<10)sample=-128;
//						if(0<=ltz && ltz<10)sample=-64;
						res[i][x+y*result.XDIM+z*result.XDIM*result.YDIM]=sample;
					}
		try(FileOutputStream fos=new FileOutputStream(result.getDataFilename(),true)){
			for(byte[]blob:res)
				fos.write(blob);
		}
		
//		for(i=0;i<res[0].length;i++) {
//			res[0][i]=(res[0][i] & 255)>50?(byte)1:0;
//			res[1][i]=(res[1][i] & 255)>50?(byte)1:0;
//		}

//		{
//		Nifti1Dataset c0=new Nifti1Dataset();
//		c0.copyHeader(result);
//		c0.setHeaderFilename("AF-GH1_20191209_c0.nii");
//		c0.setDataFilename("AF-GH1_20191209_c0.nii");
//		c0.dim[5]=0;
//		c0.writeHeader();
//		try(FileOutputStream fos=new FileOutputStream(c0.getDataFilename(),true)){
//			fos.write(res[0]);
//		}
//		}
//		{
//		Nifti1Dataset c1=new Nifti1Dataset();
//		c1.copyHeader(result);
//		c1.setHeaderFilename("AF-GH1_20191209_c1.nii");
//		c1.setDataFilename("AF-GH1_20191209_c1.nii");
//		c1.dim[5]=0;
//		c1.writeHeader();
//		try(FileOutputStream fos=new FileOutputStream(c1.getDataFilename(),true)){
//			fos.write(res[1]);
//		}
//		}
	}
	
	public static class Landmark {
		public String name;
		public double x,y,z,px,py,pz;
	}
}
