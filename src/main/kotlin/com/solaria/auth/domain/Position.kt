@Entity
@Table(name = "position")
data class Position(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var name: String,

    @OneToMany(mappedBy = "position")
    val users: MutableList<User> = mutableListOf(),

    @OneToMany(mappedBy = "position")
    val permissions: MutableList<PositionPermission> = mutableListOf()
)