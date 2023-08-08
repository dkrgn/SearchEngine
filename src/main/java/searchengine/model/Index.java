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
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(
            name = "page_id",
            nullable = false,
            referencedColumnName = "id")
    private Page page;

    @ManyToOne
    @JoinColumn(
            name = "lemma_id",
            nullable = false,
            referencedColumnName = "id")
    private Lemma lemma;

    @NotNull
    private Float ranking;
}
