package com.example.presentationservice.service;

import com.example.presentationservice.model.Topic;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ArchivationService {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final PersistenceService persistenceService;

    @Autowired
    public ArchivationService(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Scope("session")
    public Future<byte[]> getArchieve() throws IOException {

        Callable<byte[]> task = () -> {
            List<Topic> allTopics = persistenceService.getAllTopicsForArchivation();

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final ObjectMapper mapper = new ObjectMapper();

            mapper.writeValue(out, allTopics);

            final byte[] data = out.toByteArray();

            ZipOutputStream zos = new ZipOutputStream(out);
            ZipEntry entry1 = new ZipEntry("topics.json");

            entry1.setSize(data.length);
            zos.putNextEntry(entry1);
            zos.write(data);

            zos.closeEntry();
            zos.close();

            return out.toByteArray();
        };

        Future<byte[]> submit = executor.submit(task);

        return submit;
    }
}
