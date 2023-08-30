package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;


@Getter
@Setter
@Entity
@Table(name = "index_model")
public class IndexModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id",referencedColumnName = "id", nullable = false)
    private Page page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id",referencedColumnName = "id", nullable = false)
    private Lemma lemma;

    @Column(name = "lemma_rank", nullable = false)
    private float rank;

}
