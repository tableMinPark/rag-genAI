package com.genai.core.repository.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GetCollectionResponse {

    private Mappings mappings;

    private Settings settings;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Mappings {
        @JsonProperty("properties")
        private Map<String, Map<String, Object>> properties;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Settings {

        private Index index;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Index {
            @JsonProperty("replication")
            private Replication replication;
            @JsonProperty("number_of_shards")
            private String numberOfShards;
            @JsonProperty("provided_name")
            private String providedName;
            @JsonProperty("knn")
            private String knn;
            @JsonProperty("creation_date")
            private String creationDate;
            @JsonProperty("number_of_replicas")
            private String numberOfReplicas;
            @JsonProperty("uuid")
            private String uuid;
            @JsonProperty("version")
            private Version version;

            @Getter
            @Setter
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Replication {
                private String type;
            }

            @Getter
            @Setter
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Version {
                private String created;
            }
        }

    }
}
