package me.desair.tus.server.util;

import jakarta.annotation.Nonnull;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import lombok.Getter;

public class TestClock extends Clock {
  private final Instant resetHolder;
  private Instant instant;
  @Getter private final ZoneId zone;

  public TestClock(Instant fixedInstant, ZoneId zone) {
    this.resetHolder = fixedInstant;
    this.instant = fixedInstant;
    this.zone = zone;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    if (zone.equals(this.zone)) {
      return this;
    }
    return new TestClock(instant, zone);
  }

  @Override
  public long millis() {
    return instant.toEpochMilli();
  }

  @Override
  public Instant instant() {
    return instant;
  }

  public void plus(@Nonnull TemporalAmount amount) {
    instant = instant.plus(amount);
  }

  public void plusSeconds(long amount) {
    instant = instant.plusSeconds(amount);
  }

  public void plusMillis(long amount) {
    instant = instant.plusMillis(amount);
  }

  public void setInstant(@Nonnull Instant newTime) {
    instant = newTime;
  }

  public void reset() {
    instant = resetHolder;
  }

  public LocalDate getLocalDate() {
    return LocalDate.ofInstant(instant(), getZone());
  }
}
