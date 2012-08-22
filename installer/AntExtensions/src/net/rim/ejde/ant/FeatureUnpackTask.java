
package net.rim.ejde.ant;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Mkdir;
import org.apache.tools.ant.types.FileSet;

/**
 * @author psuchomel
 */
public class FeatureUnpackTask extends Task {

    private List<FileSet> fileset = new LinkedList<FileSet>();

    /* packed feature jar files to be unpacked*/
    public void addFileset(FileSet fs) {
        fileset.add(fs);
    }

    private File destinationFolder;

    /* top level folder, where are files unpacked, usually eclipse/features */
    public void setTarget(File target){
        this.destinationFolder = target;
    }

    public @Override void execute() throws BuildException {
        
        for (FileSet fs : fileset) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File basedir = ds.getBasedir();
            for (String file : ds.getIncludedFiles()) {
                String folderName = file.substring(0, file.lastIndexOf(".jar"));
                Project p = getProject();

                File targetFolder = new File(destinationFolder, folderName);

                Mkdir mkdir = (Mkdir)p.createTask("mkdir");
                mkdir.setDir(targetFolder);
                mkdir.execute();

                Expand unpack = (Expand)p.createTask("unzip");
                unpack.setDest(targetFolder);
                unpack.setSrc(new File(basedir, file));
                unpack.execute();
            }
        }
    }

}
