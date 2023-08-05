package searchengine.model;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@Entity
@Table(name = "indices")
@NoArgsConstructor
@AllArgsConstructor
public class IndexModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(
            name = "page_id",
            nullable = false,
            referencedColumnName = "id")
    private PageModel pageModel;

    @ManyToOne
    @JoinColumn(
            name = "lemma_id",
            nullable = false,
            referencedColumnName = "id")
    private LemmaModel lemmaModel;

    @NotNull
    private Float ranking;
}
