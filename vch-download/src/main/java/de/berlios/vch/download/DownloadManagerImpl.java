package de.berlios.vch.download;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeFactory;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import de.berlios.vch.config.ConfigService;
import de.berlios.vch.download.Download.Status;
import de.berlios.vch.download.jaxb.DownloadDTO;
import de.berlios.vch.download.jaxb.ObjectFactory;
import de.berlios.vch.download.sorting.ByFinishDate;
import de.berlios.vch.download.sorting.ByTitle;
import de.berlios.vch.download.sorting.SortStrategy;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.parser.IVideoPage;

@Component
@Provides(specifications= {DownloadManager.class})
public class DownloadManagerImpl implements DownloadManager, DownloadStateListener {

    @Requires
    private LogService logger;

    @Requires
    private Messages i18n;

    @Requires
    private ConfigService cs;

    private Preferences prefs;

    private List<Download> downloads = new ArrayList<Download>();

    private ExecutorService executor;

    private File dataDir;

    private Marshaller marshaller;

    private Unmarshaller unmarshaller;

    private ServiceTracker downloadFactoryTracker;

    private BundleContext ctx;

    public static List<SortStrategy> sortStrategies = new ArrayList<SortStrategy>();

    public DownloadManagerImpl(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Validate
    public void validate() {
        // add sort strategies
        sortStrategies.add(new ByTitle(i18n));
        sortStrategies.add(new ByFinishDate(i18n));

        // initialize with preferences
        prefs = cs.getUserPreferences(ctx.getBundle().getSymbolicName());
        Activator.setDefaults(prefs);
        init(prefs);
    }

    @Invalidate
    public void invalidate() {
        sortStrategies.clear();

        stop();
    }

    @Override
    public void cancelDownload(String id) {
        stopDownload(id);
        Download d = getDownload(id);
        if(d != null) {
            // cancel the download
            d.cancel();

            // remove download from active downloads
            downloads.remove(d);

            // delete the info file
            deleteInfoFile(new File(d.getLocalFile()));

            // delete the descriptor file
            deleteDescritorFile(new File(d.getLocalFile()));
        }
    }

    @Override
    public void deleteDownload(String id) {
        // get a list of all finished downloads
        List<DownloadDTO> finished = getFinishedDownloads();
        DownloadDTO download = null;
        for (DownloadDTO dto : finished) {
            if(id.equals(dto.getId())) {
                download = dto;
            }
        }

        if(download == null) {
            logger.log(LogService.LOG_WARNING, "Download does not exist " + id);
            return;
        }

        // delete the video file
        File videoFile = download.getVideoFile();
        if(videoFile != null && videoFile.exists()) {
            boolean deleted = videoFile.delete();
            if(!deleted) {
                logger.log(LogService.LOG_WARNING, "Couldn't delete file " + videoFile.getAbsolutePath());
            }
        }

        // delete the nfo file
        deleteInfoFile(videoFile);

        // delete the descriptor file
        deleteDescritorFile(videoFile);
    }

    @Override
    public void downloadItem(IVideoPage page) throws InstantiationException, IOException, URISyntaxException, PlaylistFileFoundException  {
        // create download
        Download d = createDownload(page);
        if(d == null) {
            throw new InstantiationException("No applicable downloader found");
        }

        d.addDownloadStateListener(this);
        d.setDestinationDir(dataDir);

        // add download to active downloads
        downloads.add(d);

        // persist download information
        createDescriptorFile(d);

        // start the download
        executor.submit(d);

        // create the nfo file
        try {
            createInfoFile(new File(d.getLocalFile()), d.getVideoPage());
        } catch (Throwable t) {
            logger.log(LogService.LOG_WARNING, "Couldn't write .nfo file for " + d.getLocalFile(), t);
        }
    }

    private Download createDownload(IVideoPage page) throws IOException, URISyntaxException, PlaylistFileFoundException {
        Object[] downloadFactories = downloadFactoryTracker.getServices();
        if(downloadFactories != null) {
            for (Object factoryObject : downloadFactories) {
                DownloadFactory factory = (DownloadFactory) factoryObject;
                if(factory.accept(page)) {
                    return factory.createDownload(page);
                }
            }
        }
        return null;
    }

    @Override
    public void startDownload(String id) {
        Download d = getDownload(id);
        if(d != null) {
            if(d.isStartable()) { // dont start a download twice
                d.setStatus(Status.WAITING);
                executor.submit(d);
            } else {
                logger.log(LogService.LOG_INFO, "AbstractDownload already running or queued");
            }
        }
    }

    @Override
    public void startDownloads() {
        for (Download d : downloads) {
            if(d.isStartable()) {
                d.setStatus(Status.WAITING);
                executor.submit(d);
            }
        }
    }

    @Override
    public void stopDownload(String id) {
        Download d = getDownload(id);
        if(d != null) {
            d.stop();
        }
    }

    @Override
    public void stopDownloads() {
        executor.shutdownNow();
        executor = createExecutorService();
        for (Download d : downloads) {
            if(!d.isStartable()) {
                d.stop();
            }
        }
    }

    @Override
    public void init(Preferences prefs) {
        this.prefs = prefs;
        this.prefs.addPreferenceChangeListener(new PreferenceChangeListener() {
            @Override
            public void preferenceChange(PreferenceChangeEvent evt) {
                reconfigure();
            }
        });

        reconfigure();

        // set up jaxb stuff
        try {
            ClassLoader cl = getClass().getClassLoader();
            JAXBContext jaxbCtx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName(), cl);
            marshaller = jaxbCtx.createMarshaller();
            marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
            unmarshaller = jaxbCtx.createUnmarshaller();
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't create jaxb context", e);
        }

        // create service tracker for download factories
        downloadFactoryTracker = new ServiceTracker(ctx, DownloadFactory.class.getName(), null);
        downloadFactoryTracker.open();
    }

    private void reconfigure() {
        // shutdown the current executor after all downloads are finished
        if(executor != null) {
            executor.shutdown();
        }

        // create new thread pool
        executor = createExecutorService();

        // create data directory
        dataDir = new File(prefs.get("data.dir", "data"));
        if(!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    private ExecutorService createExecutorService() {
        int numberOfConcurrentDownloads = prefs.getInt("concurrent_downloads", 2);
        return Executors.newFixedThreadPool(numberOfConcurrentDownloads);
    }

    @Override
    public void stop() {
        if(executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public List<Download> getActiveDownloads() {
        return downloads;
    }

    @Override
    public synchronized List<DownloadDTO> getFinishedDownloads() {
        File[] descriptors = dataDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".vch");
            }
        });
        List<DownloadDTO> finished = new ArrayList<DownloadDTO>(descriptors.length);
        for (File descriptor : descriptors) {
            String name = descriptor.getName();
            File videoFile = new File(descriptor.getParentFile(), name.substring(0, name.lastIndexOf('.')));
            if(videoFile.exists()) {
                try {
                    DownloadDTO dto = (DownloadDTO) unmarshaller.unmarshal(descriptor);
                    dto.setVideoFile(videoFile);
                    boolean isFinished = true;
                    for (Iterator<Download> iterator = downloads.iterator(); iterator.hasNext();) {
                        Download d = iterator.next();
                        if(d.getId().equals(dto.getId())) {
                            // this download is still active, don't add it to the list finished ones
                            isFinished = false;
                        }
                    }
                    if(isFinished) {
                        finished.add(dto);
                    }
                } catch (JAXBException e) {
                    logger.log(LogService.LOG_ERROR, "Couldn't read video descriptor " + descriptor.getAbsolutePath(), e);
                }
            }
        }

        // sort the downloads
        String className = prefs.get("sort.strategy", ByTitle.class.getName());
        for (SortStrategy sortStrategy : sortStrategies) {
            if(sortStrategy.getClass().getName().equals(className)) {
                sortStrategy.sort(finished);
            }
        }


        return finished;
    }

    private Download getDownload(String id) {
        for (Download d : downloads) {
            if(d.getId().equals(id)) {
                return d;
            }
        }

        return null;
    }

    @Override
    public void downloadStateChanged(AbstractDownload download) {
        if(download.getStatus() == Download.Status.FINISHED) {
            // remove download from active downloads
            downloads.remove(download);
        }
    }

    private void createDescriptorFile(Download d) {
        // write the download information to the file
        File downloadFile = new File(d.getLocalFile());
        File data = new File(downloadFile.getParentFile(), downloadFile.getName() + ".vch");
        if(!data.exists()) {
            try {
                DownloadDTO dto = new DownloadDTO();
                dto.setDescription(d.getVideoPage().getDescription());
                dto.setDuration(d.getVideoPage().getDuration());
                dto.setId(d.getId());
                if(d.getVideoPage().getPublishDate() != null) {
                    dto.setPublishDate(DatatypeFactory.newInstance().newXMLGregorianCalendar((GregorianCalendar)d.getVideoPage().getPublishDate()));
                }
                if(d.getVideoPage().getThumbnail() != null) {
                    dto.setThumbUri(d.getVideoPage().getThumbnail().toString());
                }
                dto.setTitle(d.getVideoPage().getTitle());
                dto.setVideoUri(d.getVideoPage().getVideoUri().toString());
                marshaller.marshal(dto, data);
            } catch(Exception e) {
                logger.log(LogService.LOG_ERROR, "Coulnd't save download data. Download will be lost after the next restart", e);
            }
        }
    }

    private void deleteDescritorFile(File videoFile) {
        if(videoFile != null) {
            File descriptor = new File(videoFile.getParentFile(), videoFile.getName() + ".vch");
            logger.log(LogService.LOG_DEBUG, "Trying to delete file " + descriptor);
            if(descriptor != null && descriptor.exists()) {
                boolean deleted = descriptor.delete();
                if(!deleted) {
                    logger.log(LogService.LOG_WARNING, "Couldn't delete file " + descriptor.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Writes a .nfo file for a given video file and description. The nfo file will have
     * the same name as the video file plus the file extension .nfo
     * @param videoFile The video file to write the nfo file for
     * @param desc The text to write into the nfo file
     * @throws IOException
     */
    protected void createInfoFile(File videoFile, IVideoPage video) throws IOException {
        if(video == null || video.getTitle() == null || video.getTitle().length() == 0) {
            logger.log(LogService.LOG_INFO, "Video has no title. No .nfo file will be written");
        }

        if(videoFile != null) {
            File nfoFile = new File(videoFile.getParentFile(), videoFile.getName() + ".nfo");
            if(!nfoFile.exists()) {
                PrintWriter pw = null;
                try {
                    //pw = new PrintWriter(nfoFile, Config.getInstance().getProperty("default.encoding"));
                    pw = new PrintWriter(nfoFile, "UTF-8");
                    pw.write(video.getTitle() + "\n");
                    if(video.getPublishDate() != null) {
                        pw.write(DateFormat.getDateTimeInstance().format(video.getPublishDate().getTime()) + "\n");
                    }
                    if(video.getDescription() != null) {
                        pw.write("\n" + video.getDescription());
                    }
                } finally {
                    if(pw != null) {
                        pw.close();
                    }
                }
            }
        } else {
            logger.log(LogService.LOG_INFO, "Video file is null. No .nfo file will be written");
        }
    }

    /**
     * Deletes a .nfo file for a given video file, if the nfo file exists
     * @param videoFile
     */
    protected void deleteInfoFile(File videoFile) {
        if(videoFile != null) {
            File nfoFile = new File(videoFile.getParentFile(), videoFile.getName() + ".nfo");
            if(nfoFile != null && nfoFile.exists()) {
                boolean deleted = nfoFile.delete();
                if(!deleted) {
                    logger.log(LogService.LOG_WARNING, "Couldn't delete file " + nfoFile.getAbsolutePath());
                }
            }
        }
    }
}
