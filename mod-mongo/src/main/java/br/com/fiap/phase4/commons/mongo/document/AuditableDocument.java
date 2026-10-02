package br.com.fiap.phase4.commons.mongo.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

@Getter
@Setter
@NoArgsConstructor
public abstract class AuditableDocument extends BaseMongoDocument {

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String lastModifiedBy;

    protected AuditableDocument(String id) {
        super(id);
    }
}
