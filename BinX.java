import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import nii.Nifti1Dataset;

public class BinX {
	public static void main(String[] args) throws Exception {
//		new File("bin").mkdir();
		var name=args[0];
		var threshold=Integer.parseInt(args[1]);
		name=name.substring(0, name.indexOf('.'));
		var source=new Nifti1Dataset(name+".nii");
		source.readHeader();
		if(source.datatype!=Nifti1Dataset.NIFTI_TYPE_UINT8)
			throw new Exception("Non-UINT8");

		byte[]blob;
		try(FileInputStream fis=new FileInputStream(source.ds_datname)){
			fis.skipNBytes((long)source.vox_offset);
			blob=fis.readNBytes(source.XDIM*source.YDIM*source.ZDIM);
		}
		for(var i=0;i<blob.length;i++)
			blob[i]=(blob[i]&255)>threshold?(byte)1:0;
		Nifti1Dataset result=new Nifti1Dataset();
		result.copyHeader(source);
		result.dim[5]=1;
//		result.setHeaderFilename("bin/"+name+"_bin_"+args[1]+".nii");
//		result.setDataFilename("bin/"+name+"_bin_"+args[1]+".nii");
		result.setHeaderFilename(name+"_bin.nii");
		result.setDataFilename(name+"_bin.nii");
		result.writeHeader();
		try(FileOutputStream fos=new FileOutputStream(result.getDataFilename(),true)){
			fos.write(blob);
		}
	}
}
