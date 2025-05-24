package dev.eventk.example.app

import dev.eventk.example.domain.EventListener
import dev.eventk.example.domain.UseCase
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType

@Configuration
@ComponentScan(
    basePackages = [
        "dev.eventk.example.domain.usecases.blocking",
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
