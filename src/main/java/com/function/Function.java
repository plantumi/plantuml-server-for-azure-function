package com.function;

import com.function.plantuml_server.DiagramResponse;
import com.function.plantuml_server.UmlExtractor;
import com.function.plantuml_server.UrlDataExtractor;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.HttpStatusType;
import com.microsoft.azure.functions.HttpResponseMessage.Builder;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import net.sourceforge.plantuml.FileFormat;

import java.io.IOException;
import java.util.Optional;

import javax.imageio.IIOException;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("convert")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");
        
        final String url = request.getQueryParameters().get("url");
        if (url == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("url query parameter is required").build();
        }
        final String encoded = UrlDataExtractor.getEncodedDiagram(url, "");
        final int idx = UrlDataExtractor.getIndex(url, 0);

        // build the UML source from the compressed request parameter
        final String uml;
        try {
            uml = UmlExtractor.getUmlSource(encoded);
        } catch (Exception e) {
            e.printStackTrace();
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Bad Request").build();
        }

        try {
            Builder response = request.createResponseBuilder(HttpStatus.OK);
            FileFormat format = resolveResponseFormat(url);
            doDiagramResponse(request, response, uml, idx, format);
            return response.build();
        } catch (IOException e) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private FileFormat resolveResponseFormat(String url) {
        if (url.contains("/svg/")) {
            return FileFormat.SVG;
        }
        if (url.contains("/xmi_argo/")) {
            return FileFormat.XMI_ARGO;
        }
        if (url.contains("/xmi/")) {
            return FileFormat.XMI_STANDARD;
        }
        if (url.contains("/xmi_star/")) {
            return FileFormat.XMI_STAR;
        }
        if (url.contains("/pdf/")) {
            return FileFormat.PDF;
        }
        if (url.contains("/txt/")) {
            return FileFormat.ATXT;
        }
        if (url.contains("/html/")) {
            return FileFormat.HTML;
        }
        if (url.contains("/html5/")) {
            return FileFormat.HTML5;
        }
        if (url.contains("/png/")) {
            return FileFormat.PNG;
        }
        if (url.contains("/img/")) {
            return FileFormat.PNG;
        }
        if (url.contains("/graphml/")) {
            return FileFormat.GRAPHML;
        }
        if (url.contains("/latex/")) {
            return FileFormat.LATEX;
        }
        if (url.contains("/preproc/")) {
            return FileFormat.PREPROC;
        }
        if (url.contains("/scxml/")) {
            return FileFormat.SCXML;
        }
        if (url.contains("/vdx/")) {
            return FileFormat.VDX;
        }
        // return svg as default
        return FileFormat.SVG;
    }

    /**
     * Send diagram response.
     *
     * @param request html request
     * @param response html response
     * @param uml textual UML diagram(s) source
     * @param idx diagram index of {@code uml} to send
     * @return 
     *
     * @throws IOException if an input or output exception occurred
     */
    private void doDiagramResponse(
        HttpRequestMessage<Optional<String>> request,
        Builder response,
        String uml,
        int idx,
        FileFormat format
    ) throws IOException {
        // generate the response
        DiagramResponse dr = new DiagramResponse(response, format, request);
        try {
            dr.sendDiagram(uml, idx);
        } catch (IIOException e) {
            // Browser has closed the connection, so the HTTP OutputStream is closed
            // Silently catch the exception to avoid annoying log
        }
    }
}
