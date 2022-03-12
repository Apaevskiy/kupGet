package kup.get.service.socket;

import io.rsocket.metadata.WellKnownMimeType;
import kup.get.config.RSocketClientBuilderImpl;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.util.Arrays;

public class SocketService {
    private UsernamePasswordMetadata metadata;
    private final MimeType mimetype = MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.getString());
    private final RSocketClientBuilderImpl config;


    public SocketService(RSocketClientBuilderImpl config) {
        this.config = config;
    }

    public Flux<String> authorize(String username, String password) {
        if(config.getRequester()!=null){
            metadata = new UsernamePasswordMetadata(username, password);
            return route("greetings").retrieveFlux(String.class);
        } else {
            return Flux.fromIterable(Arrays.asList("ROLE_TRAFFIC"));
        }
    }

    protected RSocketRequester.RequestSpec route(String s) {
        return config.getRequester().route(s).metadata(this.metadata, this.mimetype);
    }
}