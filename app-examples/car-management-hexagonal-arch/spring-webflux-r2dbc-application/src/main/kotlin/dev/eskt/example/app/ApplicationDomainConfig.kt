package dev.eskt.example.app

import dev.eskt.example.domain.EventListener
import dev.eskt.example.domain.UseCase
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType

@Configuration
@ComponentScan(
    basePackages = [
        "dev.eskt.example.domain.usecases.suspending",
    ],
    includeFilters = [
        ComponentScan.Filter(
            type = FilterType.ANNOTATION,
            classes = [
                UseCase::class,
                EventListener::class,
            ],
        ),
    ],
)
class ApplicationDomainConfig
