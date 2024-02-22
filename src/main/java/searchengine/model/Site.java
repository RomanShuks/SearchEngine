package searchengine.model;

import lombok.*;

import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Entity
@Table(name = "site")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT")
    private Integer id;

    @NonNull
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    private String status;

    @Column(name = "status_time", nullable = false)
    private Timestamp statusTime;

    @Column(columnDefinition = "TEXT", name = "last_error")
    private String lastError;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(255)")
    private String url;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String name;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    private Set<Page> pages = new HashSet<>();

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    private Set<Lemma> lemmas = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Site site = (Site) o;
        return Objects.equals(url, site.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}