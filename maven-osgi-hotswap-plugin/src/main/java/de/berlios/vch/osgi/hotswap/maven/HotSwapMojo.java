package de.berlios.vch.osgi.hotswap.maven;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal hotswap
 */
public class HotSwapMojo extends AbstractMojo {
//    /**
//     * @parameter expression="${project.artifactId}"
//     * @required
//     */
//    private String artifactId;
//    
//    /**
//     * @parameter expression="${project.groupId}"
//     * @required
//     */
//    private String groupId;
    
    /**
     * @parameter expression="${bundle.symbolicName}" default-value="${project.groupId}.${project.artifactId}"
     */
    private String symbolicName;
    
    /**
     * @parameter expression="${project.build.finalName}"
     */
    private String finalName;
    
    /**
     * @parameter expression="${project.build.directory}"
     */
    private String outputDir;
    
    public void execute() throws MojoExecutionException {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL("http://localhost:8765/").openConnection();
            con.setRequestProperty("Bundle-SymbolicName", symbolicName);
            con.setRequestProperty("URL", "file:" + outputDir + "/" + finalName + ".jar");
            int status = con.getResponseCode();
            getLog().info("Return code: " + status);
            if(status != HttpURLConnection.HTTP_OK) {
                StringBuilder sb = new StringBuilder();
                InputStream in = con.getErrorStream();
                int length = -1;
                byte[] b = new byte[1024];
                while( (length = in.read(b)) >= 0) {
                    sb.append(new String(b, 0, length));
                }
                throw new MojoExecutionException(sb.toString());
            }
            con.disconnect();
        } catch (Exception e) {
            throw new MojoExecutionException("Couldn't deploy bundle with osgi hotswap bundle", e);
        }
    }
}
