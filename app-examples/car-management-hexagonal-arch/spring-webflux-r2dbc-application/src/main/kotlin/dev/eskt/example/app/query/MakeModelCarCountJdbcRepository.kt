package dev.eskt.example.app.query

import dev.eskt.example.domain.query.MakeModelCarCount
import dev.eskt.example.domain.query.MakeModelCarCountRepository
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrNull

@Repository
interface MakeModelCarCountJdbcRepository : CrudRepository<MakeModelCarCountEntity, MakeModelCarCountEntity.Cid>, MakeModelCarCountRepository {
    override fun listAll(): List<MakeModelCarCount> {
        return findAll().map { it.toDomain() }
    }

    override fun find(make: String, model: String): MakeModelCarCount? {
        return findById(MakeModelCarCountEntity.Cid(make, model)).getOrNull()?.toDomain()
    }

    override fun save(carCount: MakeModelCarCount) {
        save(carCount.toEntity())
    }

    private fun MakeModelCarCountEntity.toDomain() = MakeModelCarCount(
        make = id.make,
        model = id.model,
        count = count,
    )

    private fun MakeModelCarCount.toEntity(): MakeModelCarCountEntity {
        return MakeModelCarCountEntity(
            id = MakeModelCarCountEntity.Cid(make = make, model = model),
            count = count,
        )
    }
}
