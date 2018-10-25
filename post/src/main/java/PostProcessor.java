import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * @author Andre Greiner-Petter
 */
public class PostProcessor {

    private static final Logger LOG = LogManager.getLogger(PostProcessor.class.getName());

    private static final int maxDocs = 137_864; // no_problems data
    private static int currentlyProcessedDocs = 0;

    private UpdateProgress progressRunnable;

    private static long startTimeMillis;

    private final Path docsPath;
    private final Path outputPath;

    public PostProcessor(String pathToDocs, String output) throws IOException {
        docsPath = Paths.get(pathToDocs);
        outputPath = Paths.get(output);
        checkPaths();
        progressRunnable = new UpdateProgress();
    }

    public void startProcessing(){
        System.out.println("Start processing files in " + docsPath.toString());
        System.out.println("Logging will be written to log file.");
        startTimeMillis = System.currentTimeMillis();

        new Thread(progressRunnable).start();

        try {
            Files
                    .walk(docsPath) // no SecurityException possible, we checked "can read" beforehand
                    .parallel()
                    .filter( p -> p.toString().endsWith(".txt") ) // only take txt and not annotation file
                    .map( p -> new SentenceSplitter(docsPath, outputPath, p) )
                    .forEach( ss -> {
                        try {
                            ss.start();
                        } catch ( RuntimeException re ){
                            LOG.info("Skipped " + ss.getFile().toString() + " because: " + re.getMessage());
                        }
                        currentlyProcessedDocs++;
                    } );
        } catch ( IOException ioe ){
            LOG.error("Cannot start walking through the directory.", ioe);
            System.out.println("I/O Error detected, process stopped.");
            ioe.printStackTrace();
        } finally {
            progressRunnable.stoppingProgressUpdates();
        }
    }

    private void checkPaths() throws NullPointerException, IllegalArgumentException, SecurityException, IOException {
        File f = docsPath.toFile();
        if ( !f.exists() ) throw new NullPointerException("Path to documents doesn't exist: " + docsPath.toString());
        if ( !f.isDirectory() ) throw new IllegalArgumentException("Path do not point to a directory!");
        if ( !f.canRead() ) throw new SecurityException("No read access to directory " + docsPath.toString());

        if ( !Files.exists(outputPath) ){
            LOG.info("Create output directory at " + outputPath.toString());
            Files.createDirectory(outputPath);
        }
    }

    private static void startPrintingProcess(){
        System.out.print("\r" + getProcessString(currentlyProcessedDocs));
    }

    private static String getProcessString(int number){
        long time = System.currentTimeMillis() - startTimeMillis;

        int status = (int)(100*(number/(double)maxDocs));
        StringBuffer lengthBuf = new StringBuffer();
        for ( int l = 0; l < status/2; l++ ){
            lengthBuf.append("=");
        }

        if ( status/2 < 50 ) lengthBuf.append(">");

        return String.format(
                " %3d%% [%-50s] %6d/%d  %02d:%02d:%02d",
                status,
                lengthBuf.toString(),
                number,
                maxDocs,
                TimeUnit.MILLISECONDS.toHours(time),
                TimeUnit.MILLISECONDS.toMinutes(time)%60,
                TimeUnit.MILLISECONDS.toSeconds(time)%60);
    }

    public static void main(String[] args) throws InterruptedException, IOException {
//        String in = "/home/andreg-p/Projects/Math2Vec/planetext/data-out/";
//        String out = "/home/andreg-p/Projects/Math2Vec/planetext/data-out-processed";
        String in = null, out = null;

        for ( int i = 0; i < args.length; i++ ){
            if ( args[i].matches("-{1,2}(h|help)") ){
                System.out.println("Please specify the following settings:");
                System.out.println("\t-i or --input  <input directory>\t specify the input directory.");
                System.out.println("\t-o or --output <output directory>\t specify the output directory.");
                return;
            } else if ( args[i].matches("-{1,2}(i|in|input)") ){
                in = args[i+1];
                i++;
                continue;
            } else if ( args[i].matches("-{1,2}(o|out|output)") ){
                out = args[i+1];
                i++;
                continue;
            }
        }
        if ( in == null || out == null ){
            System.out.println("You must specify in and output directories. Type -h for requesting manual.");
            return;
        }

        PostProcessor splitter = new PostProcessor(in, out);
        splitter.startProcessing();
        splitter.progressRunnable.stoppingProgressUpdates();
    }

    private class UpdateProgress implements Runnable {

        private boolean stopped = false;

        public void stoppingProgressUpdates(){
            stopped = true;
        }

        @Override
        public void run() {
            while(!stopped){
                try {
                    Thread.sleep(1000);
                    PostProcessor.startPrintingProcess();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(); // stop with a new line
        }
    }

}
