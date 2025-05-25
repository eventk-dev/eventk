package dev.eventk.example.app.query

import dev.eventk.example.domain.query.MakeModelCarCount
import dev.eventk.example.domain.query.MakeModelCarCountRepository
import org.springframework.data.jpa.repository.JpaRepository
import kotlin.jvm.optionals.getOrNull

interface MakeModelCarCountJpaRepository : JpaRepository<MakeModelCarCountEntity, MakeModelCarCountEntity.Id>, MakeModelCarCountRepository {
    override fun listAll(): List<MakeModelCarCount> {
        return findAll().map { it.toDomain() }
    }

    override fun find(make: String, model: String): MakeModelCarCount? {
        return findById(MakeModelCarCountEntity.Id(make, model)).getOrNull()?.toDomain()
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
            id = MakeModelCarCountEntity.Id(make = make, model = model),
            count = count,
        )
    }
}
