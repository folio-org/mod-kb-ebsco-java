package org.folio.rest.converter.common.attr;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.folio.rest.jaxrs.model.EmbargoPeriod;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class EmbargoPeriodConverterTest {

  private static final int MIN_VALUE = 1;
  private static final int MAX_VALUE = 100;

  private EmbargoPeriodConverter converter;

  @DataPoints("valid-periods")
  public static org.folio.holdingsiq.model.EmbargoPeriod[] validPeriods() {
    return Arrays.stream(EmbargoUnit.values())
      .map(unit -> createPeriod(unit.value(), randomValue()))
      .toArray(org.folio.holdingsiq.model.EmbargoPeriod[]::new);
  }

  @DataPoints("mixed-case-units")
  public static org.folio.holdingsiq.model.EmbargoPeriod[] periodsWithMixedCaseUnits() {
    return Arrays.stream(EmbargoUnit.values())
      .map(unit -> createPeriod(mixCase(unit.value()), randomValue()))
      .toArray(org.folio.holdingsiq.model.EmbargoPeriod[]::new);
  }

  @DataPoints("invalid-units")
  public static org.folio.holdingsiq.model.EmbargoPeriod[] periodsWithInvalidUnits() {
    return ArrayUtils.toArray(
      createPeriod(RandomStringUtils.insecure().nextAlphanumeric(10), randomValue()), // randomly generated unit name +
      createPeriod(null, randomValue())  // unit == null
    );
  }

  @Before
  public void setUp() {
    converter = new EmbargoPeriodConverter();
  }

  @Test
  public void testNullEmbargoPeriodConvertedToNull() {
    EmbargoPeriod result = converter.convert(null);
    assertNull(result);
  }

  @Theory
  public void testEmbargoPeriodWithValidUnitConverted(
    @FromDataPoints("valid-periods") org.folio.holdingsiq.model.EmbargoPeriod period) {
    testEmbargoPeriod(period);
  }

  @Theory
  public void testCharacterCaseOfUnitIgnored(
    @FromDataPoints("mixed-case-units") org.folio.holdingsiq.model.EmbargoPeriod period) {
    testEmbargoPeriod(period);
  }

  @Theory
  public void testInvalidEmbargoUnitConvertedToNull(
    @FromDataPoints("invalid-units") org.folio.holdingsiq.model.EmbargoPeriod period) {
    EmbargoPeriod result = converter.convert(period);

    assertNotNull(result);
    assertNull(result.getEmbargoUnit());
    assertThat(result.getEmbargoValue(), allOf(greaterThanOrEqualTo(MIN_VALUE), lessThanOrEqualTo(MAX_VALUE)));
  }

  private void testEmbargoPeriod(org.folio.holdingsiq.model.EmbargoPeriod period) {
    EmbargoPeriod result = converter.convert(period);

    assertNotNull(result);
    assertThat(result.getEmbargoUnit(), is(oneOf(EmbargoUnit.values())));
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

  private static org.folio.holdingsiq.model.EmbargoPeriod createPeriod(String unit, int value) {
    return org.folio.holdingsiq.model.EmbargoPeriod.builder()
      .embargoUnit(unit)
      .embargoValue(value).build();
  }
}
