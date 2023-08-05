package searchengine.model;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "pages", indexes = @Index(name = "path_index", columnList = "path"))
@AllArgsConstructor
@NoArgsConstructor
public class PageModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(
            name = "site_id",
            nullable = false,
            referencedColumnName = "id")
    private SiteModel siteModel;

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
            mappedBy = "pageModel",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private Set<IndexModel> indexModels;
}
