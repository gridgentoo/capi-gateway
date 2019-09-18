package at.rodrigo.api.gateway.routes;

import at.rodrigo.api.gateway.cache.RunningApiManager;
import at.rodrigo.api.gateway.entity.Api;
import at.rodrigo.api.gateway.entity.Path;
import at.rodrigo.api.gateway.processor.AuthProcessor;
import at.rodrigo.api.gateway.utils.CamelUtils;
import at.rodrigo.api.gateway.utils.Constants;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.http.conn.HttpHostConnectException;
import org.springframework.http.HttpStatus;

public class DynamicRestRouteBuilder extends RouteBuilder {

    private Api api;
    private AuthProcessor authProcessor;
    private String apiGatewayErrorEndpoint;

    private RunningApiManager runningApiManager;

    private CamelUtils camelUtils;

    public DynamicRestRouteBuilder(CamelContext context, AuthProcessor authProcessor, RunningApiManager runningApiManager, CamelUtils camelUtils, String apiGatewayErrorEndpoint, Api api) {
        super(context);
        this.api = api;
        this.authProcessor = authProcessor;
        this.runningApiManager = runningApiManager;
        this.apiGatewayErrorEndpoint = apiGatewayErrorEndpoint;
        this.camelUtils = camelUtils;
    }

    @Override
    public void configure() {
        for(Path path : api.getPaths()) {
            if(api.isSecured()) {
                from(camelUtils.buildDirectRoute(api, path))
                        .streamCaching()
                        .onException(HttpHostConnectException.class)
                        .setHeader(Constants.REASON_CODE_HEADER, constant(HttpStatus.SERVICE_UNAVAILABLE.value()))
                        .setHeader(Constants.REASON_MESSAGE_HEADER, constant("API NOT AVAILABLE"))
                        .setHeader(Constants.ROUTE_ID_HEADER,constant(api.getContext() + path.getPath() + "-" + path.getVerb()))
                        .removeHeader(Constants.VALID_HEADER)
                        .continued(true)
                        .toF(Constants.FAIL_REST_ENDPOINT_OBJECT, apiGatewayErrorEndpoint)
                        .removeHeader(Constants.REASON_CODE_HEADER)
                        .removeHeader(Constants.REASON_MESSAGE_HEADER)
                        .removeHeader(Constants.ROUTE_ID_HEADER)
                        .end()
                        .setHeader(Constants.JSON_WEB_KEY_SIGNATURE_ENDPOINT_HEADER, constant(api.getJwsEndpoint()))
                        .setHeader(Constants.BLOCK_IF_IN_ERROR_HEADER, constant(path.isBlockIfInError()))
                        .process(authProcessor)

                        .choice()
                        .when(simple("${in.headers.valid} == true"))
                        .toF(camelUtils.getCamelHttpEndpoint(api), (api.getEndpoint() + path.getPath()))
                        .removeHeader(Constants.VALID_HEADER)
                        .log("Response from " + api.getName() + ": ${body}")
                        .convertBodyTo(String.class)

                        .otherwise()
                        .setHeader(Constants.ROUTE_ID_HEADER,constant(api.getContext() + path.getPath() + "-" + path.getVerb()))
                        .toF(Constants.FAIL_REST_ENDPOINT_OBJECT, apiGatewayErrorEndpoint)
                        .removeHeader(Constants.REASON_CODE_HEADER)
                        .removeHeader(Constants.REASON_MESSAGE_HEADER)
                        .removeHeader(Constants.ROUTE_ID_HEADER)
                        .log("ERROR on " + api.getName() + ": ${body}")
                        .convertBodyTo(String.class)
                        .end()
                        .setId(camelUtils.buildDirectRouteID(api, path));
            } else {
                from(camelUtils.buildDirectRoute(api, path))
                        .streamCaching()
                        .onException(HttpHostConnectException.class)
                        .setHeader(Constants.REASON_CODE_HEADER, constant(HttpStatus.SERVICE_UNAVAILABLE.value()))
                        .setHeader(Constants.REASON_MESSAGE_HEADER, constant("API NOT AVAILABLE"))
                        .setHeader(Constants.ROUTE_ID_HEADER,constant(api.getContext() + path.getPath() + "-" + path.getVerb()))
                        .continued(true)
                        .toF(Constants.FAIL_REST_ENDPOINT_OBJECT, apiGatewayErrorEndpoint)
                        .removeHeader(Constants.REASON_CODE_HEADER)
                        .removeHeader(Constants.REASON_MESSAGE_HEADER)
                        .removeHeader(Constants.ROUTE_ID_HEADER)
                        .end()
                        .toF(camelUtils.getCamelHttpEndpoint(api), (api.getEndpoint() + path.getPath()))
                        .log("Response from "+ api.getName() + ": ${body}")
                        .convertBodyTo(String.class)

                        .end()
                        .setId(camelUtils.buildDirectRouteID(api, path));
            }

            runningApiManager.runApi(api.getContext() + path.getPath() + "-" + path.getVerb(), api.getId(), path);

            switch(path.getVerb()) {
                case GET:
                    rest()
                            .get("/" + api.getContext() + path.getPath()).enableCORS(true)
                            .route()
                            .to(Constants.DIRECT_ROUTE_IDENTIFIER + api.getContext() + path.getPath() + "-" + path.getVerb())
                            .streamCaching()
                            .end()
                            .marshal().json(JsonLibrary.Jackson)
                            .convertBodyTo(String.class)
                            .end()
                            .setId(camelUtils.buildRestRouteID(api, path));
                    break;
                case POST:
                    rest()
                            .post("/" + api.getContext() + path.getPath()).enableCORS(true)
                            .route()
                            .to(Constants.DIRECT_ROUTE_IDENTIFIER + api.getContext() + path.getPath() + "-" + path.getVerb())
                            .streamCaching()
                            .end()
                            .marshal().json(JsonLibrary.Jackson)
                            .convertBodyTo(String.class)
                            .end()
                            .setId(camelUtils.buildRestRouteID(api, path));
                    break;
                case PUT:
                    rest()
                            .put("/" + api.getContext() + path.getPath()).enableCORS(true)
                            .route()
                            .to(Constants.DIRECT_ROUTE_IDENTIFIER + api.getContext() + path.getPath() + "-" + path.getVerb())
                            .streamCaching()
                            .end()
                            .marshal().json(JsonLibrary.Jackson)
                            .convertBodyTo(String.class)
                            .end()
                            .setId(camelUtils.buildRestRouteID(api, path));
                    break;
                case DELETE:
                    rest()
                            .delete("/" + api.getContext() + path.getPath()).enableCORS(true)
                            .route()
                            .to(Constants.DIRECT_ROUTE_IDENTIFIER + api.getContext() + path.getPath() + "-" + path.getVerb())
                            .streamCaching()
                            .end()
                            .marshal().json(JsonLibrary.Jackson)
                            .convertBodyTo(String.class)
                            .end()
                            .setId(camelUtils.buildRestRouteID(api, path));
                    break;
                default:
                    log.error("PATH NOT AVAILABLE: {}", path.getVerb());
                    break;
            }
        }
    }
}