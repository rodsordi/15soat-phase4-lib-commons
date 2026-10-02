package br.com.fiap.phase4.commons.mongo;

import br.com.fiap.phase4.commons.mongo.converter.ZonedDateTimeReadConverter;
import br.com.fiap.phase4.commons.mongo.converter.ZonedDateTimeWriteConverter;
import br.com.fiap.phase4.commons.mongo.document.BaseMongoDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MongoDB Commons Tests")
class MongoCommonsTest {

    static class TestDocument extends BaseMongoDocument {
        TestDocument() {
            super();
        }
        TestDocument(String id) {
            super(id);
        }
    }

    @Nested
    @DisplayName("BaseMongoDocument Tests")
    class DocumentTests {

        @Test
        @DisplayName("Should initialize ID and timestamps")
        void shouldInitializeFields() {
            var doc = new TestDocument("doc-123");
            assertThat(doc.getId()).isEqualTo("doc-123");
            assertThat(doc.getCreatedAt()).isNotNull();
            assertThat(doc.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("ZonedDateTime Converter Tests")
    class ConverterTests {

        @Test
        @DisplayName("Should convert ZonedDateTime to Date and back to ZonedDateTime in UTC")
        void shouldConvertZonedDateTime() {
            var writeConverter = new ZonedDateTimeWriteConverter();
            var readConverter = new ZonedDateTimeReadConverter();

            ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
            Date date = writeConverter.convert(now);
            ZonedDateTime convertedBack = readConverter.convert(date);

            assertThat(convertedBack.toInstant().getEpochSecond())
                    .isEqualTo(now.toInstant().getEpochSecond());
        }
    }
}
