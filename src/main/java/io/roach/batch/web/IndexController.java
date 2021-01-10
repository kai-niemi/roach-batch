package io.roach.batch.web;

import java.io.IOException;
import java.util.Collections;

import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.roach.batch.config.ProxyProfileEnabled;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@ProxyProfileEnabled
public class IndexController {
    @GetMapping
    public ResponseEntity<MessageModel> index() throws IOException {
        MessageModel index = new MessageModel("Welcome to Roach Batch");

        // Spring boot actuators for observability / monitoring
        index.add(Link.of(
                ServletUriComponentsBuilder
                        .fromCurrentContextPath()
                        .pathSegment("actuator")
                        .buildAndExpand()
                        .toUriString()
        ).withRel(LinkRels.ACTUATOR_REL));

        index.add(linkTo(methodOn(IndexController.class)
                .index())
                .withSelfRel());

        index.add(linkTo(methodOn(DownloadController.class)
                .downloadResource(Collections.emptyMap()))
                .withRel(LinkRels.DOWNLOAD_REL));

        return new ResponseEntity<>(index, HttpStatus.OK);
    }
}
