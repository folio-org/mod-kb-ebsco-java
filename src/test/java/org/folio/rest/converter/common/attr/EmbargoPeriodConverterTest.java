package org.folio.rest.converter.common.attr;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.folio.holdingsiq.model.EmbargoPeriod;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class EmbargoPeriodConverterTest {

  private static final int MIN_VALUE = 1;
  private static final int MAX_VALUE = 100;

  private final EmbargoPeriodConverter converter = new EmbargoPeriodConverter();

  @ParameterizedTest
  @MethodSource("validPeriods")
  void testEmbargoPeriodWithValidUnitConverted(EmbargoPeriod period) {
    testEmbargoPeriod(period);
  }

  @ParameterizedTest
  @MethodSource("periodsWithMixedCaseUnits")
  void testCharacterCaseOfUnitIgnored(EmbargoPeriod period) {
    testEmbargoPeriod(period);
  }

  @ParameterizedTest
  @MethodSource("periodsWithInvalidUnits")
  void testInvalidEmbargoUnitConvertedToNull(EmbargoPeriod period) {
    var result = converter.convert(period);

    assertNotNull(result);
    assertNull(result.getEmbargoUnit());
    assertThat(result.getEmbargoValue(), allOf(greaterThanOrEqualTo(MIN_VALUE), lessThanOrEqualTo(MAX_VALUE)));
  }

  @Test
  void nullEmbargoPeriodConvertedToNull() {
    var result = converter.convert(null);
    assertNull(result);
  }

  private static Stream<EmbargoPeriod> validPeriods() {
    return Arrays.stream(EmbargoUnit.values())
      .map(unit -> createPeriod(unit.value(), randomValue()));
  }

  private static Stream<EmbargoPeriod> periodsWithMixedCaseUnits() {
    return Arrays.stream(EmbargoUnit.values())
      .map(unit -> createPeriod(mixCase(unit.value()), randomValue()));
  }

  private static Stream<EmbargoPeriod> periodsWithInvalidUnits() {
    return Arrays.stream(ArrayUtils.toArray(
      createPeriod(RandomStringUtils.insecure().nextAlphanumeric(10), randomValue()),
      createPeriod(null, randomValue())
    ));
  }

  private void testEmbargoPeriod(EmbargoPeriod period) {
    var result = converter.convert(period);

    assertNotNull(result);
    assertThat(result.getEmbargoUnit(), oneOf(EmbargoUnit.values()));
    assertThat(result.getEmbargoValue(), allOf(greaterThanOrEqualTo(MIN_VALUE), lessThanOrEqualTo(MAX_VALUE)));
  }

  private static String mixCase(String value) {
    char[] result = value.toCharArray();

    for (int i = 0; i < result.length; i++) {
      boolean toUpperCase = RandomUtils.insecure().randomBoolean();

      result[i] = toUpperCase ? Character.toUpperCase(result[i]) : Character.toLowerCase(result[i]);
    }

    return String.valueOf(result);
  }

  private static int randomValue() {
    return RandomUtils.insecure().randomInt(MIN_VALUE, MAX_VALUE);
  }

  private static EmbargoPeriod createPeriod(String unit, int value) {
    return EmbargoPeriod.builder()
      .embargoUnit(unit)
      .embargoValue(value).build();
  }
}
