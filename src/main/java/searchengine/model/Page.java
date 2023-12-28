package searchengine.model;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "pages", indexes = @javax.persistence.Index(name = "path_index", columnList = "path, site_id", unique = true))
@AllArgsConstructor
@NoArgsConstructor
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(
            name = "site_id",
            nullable = false,
            referencedColumnName = "id")
    private Site site;

    @NotNull
    @Column(columnDefinition = "VARCHAR(255)")
    private String path;

    @NotNull
    @Digits(integer = 3, fraction = 0)
    private Integer code;

    @NotNull
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    @OneToMany(
            mappedBy = "page",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private Set<Index> indices;
}
