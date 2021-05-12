
public class Mtx {
	public static float[][] mult(float a[][],float b[][]) throws Exception {
		int ah=a.length;
		int aw=a[0].length;
		int bh=b.length;
		int bw=b[0].length;
		if(aw!=bh)throw new Exception("Mtx.mult");
		float retval[][]=new float[ah][bw];
		for(int y=0;y<ah;y++)
			for(int x=0;x<bw;x++)
				for(int i=0;i<aw;i++)
					retval[y][x]+=a[y][i]*b[i][x];
		return retval;
	}
	
	public static float[][] tpose(float m[][]){
		int mh=m.length;
		int mw=m[0].length;
		float retval[][]=new float[mw][mh];
		for(int x=0;x<mw;x++)
			for(int y=0;y<mh;y++)
				retval[x][y]=m[y][x];
		return retval;
	}
	
	public static float[][] inv4x4(float m[][]) throws Exception {
		if(m.length!=4)throw new Exception("Mtx.inv4x4");
		for(int i=0;i<4;i++)if(m[i].length!=4)throw new Exception("Mtx.inv4x4");
		
		float s0=m[0][0]*m[1][1]-m[1][0]*m[0][1];
        float s1=m[0][0]*m[1][2]-m[1][0]*m[0][2];
        float s2=m[0][0]*m[1][3]-m[1][0]*m[0][3];
        float s3=m[0][1]*m[1][2]-m[1][1]*m[0][2];
        float s4=m[0][1]*m[1][3]-m[1][1]*m[0][3];
        float s5=m[0][2]*m[1][3]-m[1][2]*m[0][3];

        float c5=m[2][2]*m[3][3]-m[3][2]*m[2][3];
        float c4=m[2][1]*m[3][3]-m[3][1]*m[2][3];
        float c3=m[2][1]*m[3][2]-m[3][1]*m[2][2];
        float c2=m[2][0]*m[3][3]-m[3][0]*m[2][3];
        float c1=m[2][0]*m[3][2]-m[3][0]*m[2][2];
        float c0=m[2][0]*m[3][1]-m[3][0]*m[2][1];

        float det=s0*c5-s1*c4+s2*c3+s3*c2-s4*c1+s5*c0;
        //
        float invdet=1/det;
        return new float[][]{
                             {( m[1][1]*c5-m[1][2]*c4+m[1][3]*c3)*invdet,
                              (-m[0][1]*c5+m[0][2]*c4-m[0][3]*c3)*invdet,
                              ( m[3][1]*s5-m[3][2]*s4+m[3][3]*s3)*invdet,
                              (-m[2][1]*s5+m[2][2]*s4-m[2][3]*s3)*invdet},

                             {(-m[1][0]*c5+m[1][2]*c2-m[1][3]*c1)*invdet,
                              ( m[0][0]*c5-m[0][2]*c2+m[0][3]*c1)*invdet,
                              (-m[3][0]*s5+m[3][2]*s2-m[3][3]*s1)*invdet,
                              ( m[2][0]*s5-m[2][2]*s2+m[2][3]*s1)*invdet},

                             {( m[1][0]*c4-m[1][1]*c2+m[1][3]*c0)*invdet,
                              (-m[0][0]*c4+m[0][1]*c2-m[0][3]*c0)*invdet,
                              ( m[3][0]*s4-m[3][1]*s2+m[3][3]*s0)*invdet,
                              (-m[2][0]*s4+m[2][1]*s2-m[2][3]*s0)*invdet},

                             {(-m[1][0]*c3+m[1][1]*c1-m[1][2]*c0)*invdet,
                              ( m[0][0]*c3-m[0][1]*c1+m[0][2]*c0)*invdet,
                              (-m[3][0]*s3+m[3][1]*s1-m[3][2]*s0)*invdet,
                              ( m[2][0]*s3-m[2][1]*s1+m[2][2]*s0)*invdet}};
	}
}
