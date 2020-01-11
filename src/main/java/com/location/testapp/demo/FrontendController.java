package com.location.testapp.demo;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.WritableResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

import com.google.cloud.vision.v1.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class FrontendController {

    @Autowired
    private ImageAnnotatorClient annotatorClient;

    // We need the ApplicationContext in order to create a new Resource.
    @Autowired
    private ApplicationContext context;

    // We need to know the Project ID, because it's Cloud Storage bucket name
    @Autowired
    private GcpProjectIdProvider projectIdProvider;

    private AnnotateImageResponse analyzeImage(String uri) {
        // After the image was written to GCS, analyze it with the GCS URI.
        // Note: It's also possible to analyze an image embedded in the
        // request as a Base64 encoded payload.
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ImageSource imgSrc = ImageSource.newBuilder()
                .setGcsImageUri(uri).build();
        Image img = Image.newBuilder().setSource(imgSrc).build();
        Feature feature = Feature.newBuilder()
                .setType(Feature.Type.LABEL_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest
                .newBuilder()
                .addFeatures(feature).setImage(img)
                .build();

        requests.add(request);
        BatchAnnotateImagesResponse responses =
                annotatorClient.batchAnnotateImages(requests);
        // We send in one image, expecting just one response in batch
        AnnotateImageResponse response = responses.getResponses(0);

        System.out.println(response);
        return response;
    }


    @PostMapping("/post")
    public ResponseEntity<String>  post(
            @RequestParam(name="file", required=false) MultipartFile file)
            throws IOException {
        String filename = null;

        System.out.println("file.getOriginalFilename() = " + file.getOriginalFilename());
        
        if (file != null && !file.isEmpty()
                && Objects.equals(file.getContentType(), "image/jpeg")) {

            // Bucket ID is our Project ID
            String bucket = "gs://" + projectIdProvider.getProjectId();

            // Generate a random file name
            filename = UUID.randomUUID().toString() + ".jpg";
            WritableResource resource = (WritableResource)
                    context.getResource(bucket + "/" + filename);

            // Write the file to Cloud Storage using WritableResource
            try (OutputStream os = resource.getOutputStream()) {
                os.write(file.getBytes());
            }

            // After written to GCS, analyze the image.
            AnnotateImageResponse response =  analyzeImage(bucket + "/" + filename);

            Map<String, Float> imageLabels =
                    response.getLabelAnnotationsList()
                            .stream()
                            .collect(
                                    Collectors.toMap(
                                            EntityAnnotation::getDescription,
                                            EntityAnnotation::getScore,
                                            (u, v) -> {
                                                throw new IllegalStateException(String.format("Duplicate key %s", u));
                                            },
                                            LinkedHashMap::new));

            String value = imageLabels.toString();

            return new ResponseEntity<String>(value,HttpStatus.OK);
        }

        return new ResponseEntity<String>("error",HttpStatus.OK);
    }

}
