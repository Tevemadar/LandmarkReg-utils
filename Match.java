import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import nii.Nifti1Dataset;
import parsers.JSON;

public class Match {
	public static void main(String[] args) throws Exception {
		Nifti1Dataset target=new Nifti1Dataset(args[0]);
		target.readHeader();
		var blob=new byte[target.XDIM*target.YDIM*target.ZDIM];
//		new File("matchch0bin").mkdir();
		var name=args[1];
		name=name.substring(0, name.indexOf('.'));
		var source=new Nifti1Dataset(name+".nii");
		source.readHeader();
		if(source.datatype!=Nifti1Dataset.NIFTI_TYPE_UINT8 || target.datatype!=Nifti1Dataset.NIFTI_TYPE_UINT8)
			throw new Exception("Non-UINT8");

//		byte[][]trg=new byte[target.dim[5]][];
//		try(FileInputStream fis=new FileInputStream(target.ds_datname)){
//			fis.skipNBytes((long)target.vox_offset);
//			for(int i=0;i<trg.length;i++)
//				trg[i]=fis.readNBytes(target.XDIM*target.YDIM*target.ZDIM);
//			if(fis.available()>0)throw new Exception();
//		}
		byte[]src;
		try(FileInputStream fis=new FileInputStream(source.ds_datname)){
			fis.skipNBytes((long)source.vox_offset);
			src=fis.readNBytes(source.XDIM*source.YDIM*source.ZDIM);
		}
		
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
		
		Nifti1Dataset result=new Nifti1Dataset();
		result.copyHeader(target);
		result.dim[5]=1;
//		result.setHeaderFilename("matchch0bin/"+name+"_match.nii");
//		result.setDataFilename("matchch0bin/"+name+"_match.nii");
//		result.setHeaderFilename("matchch0bin/"+name+"_bin.nii");
//		result.setDataFilename("matchch0bin/"+name+"_bin.nii");
		result.setHeaderFilename(name+"_match.nii");
		result.setDataFilename(name+"_match.nii");
		result.writeHeader();
			for(int z=0;z<result.ZDIM;z++)
				for(int y=0;y<result.YDIM;y++)
					for(int x=0;x<result.XDIM;x++) {
//						int trgsample=0;
						int ltx=x,
							lty=target.ZDIM-z,
							ltz=y;
						int tx=ltx,
							ty=ltz,
							tz=target.ZDIM-lty;
//						if(tx>=0 && tx<target.XDIM && ty>=0 && ty<target.YDIM && tz>=0 && tz<target.ZDIM)
//							trgsample=trg[i][tx+ty*target.XDIM+tz*target.XDIM*target.YDIM] & 255;
		            	int lsx=(int)(ltx*trf[0][0]+lty*trf[1][0]+ltz*trf[2][0]+trf[3][0]);
		            	int lsy=(int)(ltx*trf[0][1]+lty*trf[1][1]+ltz*trf[2][1]+trf[3][1]);
		            	int lsz=(int)(ltx*trf[0][2]+lty*trf[1][2]+ltz*trf[2][2]+trf[3][2]);
						int srcsample=0;
						int sx=lsx,
							sy=lsz,
							sz=source.ZDIM-lsy;
						if(sx>=0 && sx<source.XDIM && sy>=0 && sy<source.YDIM && sz>=0 && sz<source.ZDIM)
							srcsample=src[sx+sy*source.XDIM+sz*source.XDIM*source.YDIM] & 255;
						byte sample=(byte)srcsample;
//						byte sample=srcsample>50?(byte)1:0;
//						if(0<=ltx && ltx<10)sample=64;
//						if(0<=lty && lty<10)sample=-128;
//						if(0<=ltz && ltz<10)sample=-64;
						blob[x+y*result.XDIM+z*result.XDIM*result.YDIM]=sample;
					}
		
		try(FileOutputStream fos=new FileOutputStream(result.getDataFilename(),true)){
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
