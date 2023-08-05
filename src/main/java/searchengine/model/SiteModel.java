package searchengine.model;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "sites")
@AllArgsConstructor
@NoArgsConstructor
public class SiteModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    @Enumerated(EnumType.STRING)
    private Status status;

    @NotNull
    @Column(
            name = "status_time",
            columnDefinition = "DATETIME")
    private LocalDateTime dateTime;

    @Column(
            name = "last_error",
            columnDefinition = "TEXT")
    private String lastError;

    @NotNull
    @Column(columnDefinition = "VARCHAR(255)")
    private String url;

    @NotNull
    @Column(columnDefinition = "VARCHAR(255)")
    private String name;

    @OneToMany(
            mappedBy = "siteModel",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private Set<PageModel> pageModels;

    @OneToMany(
            mappedBy = "siteModel",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private Set<LemmaModel> lemmaModels;

}
