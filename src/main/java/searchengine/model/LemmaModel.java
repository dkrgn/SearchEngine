package searchengine.model;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "lemmas")
@NoArgsConstructor
@AllArgsConstructor
public class LemmaModel {

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
    private String lemma;

    @NotNull
    private Integer frequency;

    @OneToMany(
            mappedBy = "lemmaModel",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private Set<IndexModel> indexModels;
}
