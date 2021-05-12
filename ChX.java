import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import nii.Nifti1Dataset;

public class ChX {
	public static void main(String[] args) throws Exception {
//		new File("ch").mkdir();
		var name=args[0];
		name=name.substring(0, name.indexOf('.'));
		var source=new Nifti1Dataset(name+".nii");
		source.readHeader();
		if(source.datatype!=Nifti1Dataset.NIFTI_TYPE_UINT8)
			throw new Exception("Non-UINT8");

		byte[]blob;
		try(FileInputStream fis=new FileInputStream(source.ds_datname)){
			fis.skipNBytes((long)source.vox_offset);
			fis.skipNBytes(Long.parseLong(args[1])*source.XDIM*source.YDIM*source.ZDIM);
			blob=fis.readNBytes(source.XDIM*source.YDIM*source.ZDIM);
		}
		Nifti1Dataset result=new Nifti1Dataset();
		result.copyHeader(source);
		result.dim[5]=1;
//		result.setHeaderFilename("ch/"+name+"_ch"+args[1]+".nii");
//		result.setDataFilename("ch/"+name+"_ch"+args[1]+".nii");
		result.setHeaderFilename(name+"_ch.nii");
		result.setDataFilename(name+"_ch.nii");
		result.writeHeader();
		try(FileOutputStream fos=new FileOutputStream(result.getDataFilename(),true)){
			fos.write(blob);
		}
	}
}
