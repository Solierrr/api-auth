@Entity
@Table(name = "technician")
data class Technician(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var specialty: String,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    var person: Person
)