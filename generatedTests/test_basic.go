package main

import (
)

func test_complexMagnitude() {

	// Test case 1
	584@param#e.Real := -1.0
	585@param#e.Img := 1.0
    result := complexMagnitude(584@param#e.Real, 585@param#e.Img)
	if result != 1.0000000000000002 {
	    t.Errorf("test_complexMagnitude1 failed")
	}
	
}

func test_combinedBitwise() {

	// Test case 1
	586@param#a := 28
	587@param#b := 300
    result := combinedBitwise(586@param#a, 587@param#b)
	if result != 288 {
	    t.Errorf("test_combinedBitwise1 failed")
	}
	
	// Test case 2
	586@param#a := -1
	587@param#b := 1
    result := combinedBitwise(586@param#a, 587@param#b)
	if result != 1 {
	    t.Errorf("test_combinedBitwise2 failed")
	}
	
	// Test case 3
	586@param#a := 0
	587@param#b := 0
    result := combinedBitwise(586@param#a, 587@param#b)
	if result != 0 {
	    t.Errorf("test_combinedBitwise3 failed")
	}
	
}

func test_basicComplexOperations() {

	// Test case 1
	590@param#b.Real := -1.0
	589@param#a.Img := 1.0
	588@param#a.Real := -1.0
	591@param#b.Img := -1.2420144738406226E232
    result := basicComplexOperations(590@param#b.Real, 589@param#a.Img, 588@param#a.Real, 591@param#b.Img)
	if result != (1.0000000000002276, 1.2420144738406226E232) {
	    t.Errorf("test_basicComplexOperations1 failed")
	}
	
	// Test case 2
	590@param#b.Real := NaN
	589@param#a.Img := -1.237940039285943E27
	588@param#a.Real := NaN
	591@param#b.Img := 7.662477704332927E53
    result := basicComplexOperations(590@param#b.Real, 589@param#a.Img, 588@param#a.Real, 591@param#b.Img)
	if result != (-7.46610894802906E-301, -1.8347988927928908E106) {
	    t.Errorf("test_basicComplexOperations2 failed")
	}
	
	// Test case 3
	590@param#b.Real := 2.0000000000004556
	589@param#a.Img := 1.0000000000002274
	588@param#a.Real := 1.0
	591@param#b.Img := 1.000000000000001
    result := basicComplexOperations(590@param#b.Real, 589@param#a.Img, 588@param#a.Real, 591@param#b.Img)
	if result != (1.0, 1.0000000000002274) {
	    t.Errorf("test_basicComplexOperations3 failed")
	}
	
}

func test_compareElement() {

	// Test case 1
	595@param#index := 0
	594@param#array := 2
	592@param#array:len := -9223372036854775808
	596@param#value := -2
    result := compareElement(595@param#index, 594@param#array, 592@param#array:len, 596@param#value)
	if result != -1 {
	    t.Errorf("test_compareElement1 failed")
	}
	
	// Test case 2
	595@param#index := 193
	594@param#array := 1
	592@param#array:len := 0
	596@param#value := 1
    result := compareElement(595@param#index, 594@param#array, 592@param#array:len, 596@param#value)
	if result != 0 {
	    t.Errorf("test_compareElement2 failed")
	}
	
	// Test case 3
	595@param#index := 1
	594@param#array := 2
	592@param#array:len := 0
	596@param#value := -1
    result := compareElement(595@param#index, 594@param#array, 592@param#array:len, 596@param#value)
	if result != 1 {
	    t.Errorf("test_compareElement3 failed")
	}
	
	// Test case 4
	595@param#index := -32801
	594@param#array := 1
	592@param#array:len := -23
	596@param#value := 0
    result := compareElement(595@param#index, 594@param#array, 592@param#array:len, 596@param#value)
	if result != -1 {
	    t.Errorf("test_compareElement4 failed")
	}
	
	// Test case 5
	595@param#index := 0
	594@param#array := 1
	592@param#array:len := -2
	596@param#value := -9223372036854775808
    result := compareElement(595@param#index, 594@param#array, 592@param#array:len, 596@param#value)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_compareElement5 failed")
	}
	
}

func test_compareAge() {

	// Test case 1
	603@param#value := 6643336369142595584
	600@param#people:len := 0
	601@param#people := 1
	602@param#index := 0
    result := compareAge(603@param#value, 600@param#people:len, 601@param#people, 602@param#index)
	if result != -1 {
	    t.Errorf("test_compareAge1 failed")
	}
	
	// Test case 2
	603@param#value := 4
	600@param#people:len := 1
	601@param#people := 1
	602@param#index := 1
    result := compareAge(603@param#value, 600@param#people:len, 601@param#people, 602@param#index)
	if result != 0 {
	    t.Errorf("test_compareAge2 failed")
	}
	
	// Test case 3
	603@param#value := -2420396142951086642
	600@param#people:len := 2
	601@param#people := 2
	602@param#index := 0
    result := compareAge(603@param#value, 600@param#people:len, 601@param#people, 602@param#index)
	if result != 1 {
	    t.Errorf("test_compareAge3 failed")
	}
	
	// Test case 4
	603@param#value := 4
	600@param#people:len := 39966
	601@param#people := 2
	602@param#index := -4
    result := compareAge(603@param#value, 600@param#people:len, 601@param#people, 602@param#index)
	if result != -1 {
	    t.Errorf("test_compareAge4 failed")
	}
	
	// Test case 5
	603@param#value := 6878531882182015669
	600@param#people:len := 1
	601@param#people := 2
	602@param#index := 4
    result := compareAge(603@param#value, 600@param#people:len, 601@param#people, 602@param#index)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_compareAge5 failed")
	}
	
}

func test_nestedBitwise() {

	// Test case 1
	610@param#b := 1
	609@param#a := 1
    result := nestedBitwise(610@param#b, 609@param#a)
	if result != 1 {
	    t.Errorf("test_nestedBitwise1 failed")
	}
	
	// Test case 2
	610@param#b := 0
	609@param#a := 0
    result := nestedBitwise(610@param#b, 609@param#a)
	if result != 0 {
	    t.Errorf("test_nestedBitwise2 failed")
	}
	
	// Test case 3
	610@param#b := -1
	609@param#a := 0
    result := nestedBitwise(610@param#b, 609@param#a)
	if result != 0 {
	    t.Errorf("test_nestedBitwise3 failed")
	}
	
	// Test case 4
	610@param#b := 0
	609@param#a := -1
    result := nestedBitwise(610@param#b, 609@param#a)
	if result != -1 {
	    t.Errorf("test_nestedBitwise4 failed")
	}
	
}

func test_complexComparison() {

	// Test case 1
	614@param#b.Img := 1.2259964326927157E55
	612@param#a.Img := -7.796251209123101E289
	613@param#b.Real := -1.3625471488029916E-107
	611@param#a.Real := -1.659806227552775E181
    result := complexComparison(614@param#b.Img, 612@param#a.Img, 613@param#b.Real, 611@param#a.Real)
	if result != string!val!-780891085 {
	    t.Errorf("test_complexComparison1 failed")
	}
	
	// Test case 2
	614@param#b.Img := -1.0000000000002274
	612@param#a.Img := 1.0000000000002274
	613@param#b.Real := -1.0000000000002274
	611@param#a.Real := 6.210072369205657E231
    result := complexComparison(614@param#b.Img, 612@param#a.Img, 613@param#b.Real, 611@param#a.Real)
	if result != string!val!1187732373 {
	    t.Errorf("test_complexComparison2 failed")
	}
	
	// Test case 3
	614@param#b.Img := -4.212491666743246E65
	612@param#a.Img := 1.03397576569152E-25
	613@param#b.Real := -1.579365082794356E-176
	611@param#a.Real := -2.675548521739685E-197
    result := complexComparison(614@param#b.Img, 612@param#a.Img, 613@param#b.Real, 611@param#a.Real)
	if result != string!val!1771265876 {
	    t.Errorf("test_complexComparison3 failed")
	}
	
}

func test_advancedBitwise() {

	// Test case 1
	615@param#a := 1
	616@param#b := 1
    result := advancedBitwise(615@param#a, 616@param#b)
	if result != 0 {
	    t.Errorf("test_advancedBitwise1 failed")
	}
	
	// Test case 2
	616@param#b := 1
	615@param#a := 0
    result := advancedBitwise(616@param#b, 615@param#a)
	if result != 0 {
	    t.Errorf("test_advancedBitwise2 failed")
	}
	
	// Test case 3
	615@param#a := 2
	616@param#b := 0
    result := advancedBitwise(615@param#a, 616@param#b)
	if result != 1 {
	    t.Errorf("test_advancedBitwise3 failed")
	}
	
}

func test_integerOperations() {

	// Test case 1
	617@param#a := 0
	618@param#b := 0
    result := integerOperations(617@param#a, 618@param#b)
	if result != 0 {
	    t.Errorf("test_integerOperations1 failed")
	}
	
	// Test case 2
	617@param#a := 0
	618@param#b := 1
    result := integerOperations(617@param#a, 618@param#b)
	if result != -1 {
	    t.Errorf("test_integerOperations2 failed")
	}
	
	// Test case 3
	617@param#a := 1
	618@param#b := 0
    result := integerOperations(617@param#a, 618@param#b)
	if result != 1 {
	    t.Errorf("test_integerOperations3 failed")
	}
	
}

func test_mixedOperations() {

	// Test case 1
	620@param#b := 1.0
	619@param#a := 10
    result := mixedOperations(620@param#b, 619@param#a)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_mixedOperations1 failed")
	}
	
	// Test case 2
	620@param#b := 1.0
	619@param#a := 0
    result := mixedOperations(620@param#b, 619@param#a)
	if result != 1.0000000000000002 {
	    t.Errorf("test_mixedOperations2 failed")
	}
	
	// Test case 3
	619@param#a := 1
	620@param#b := NaN
    result := mixedOperations(619@param#a, 620@param#b)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_mixedOperations3 failed")
	}
	
}

func test_nestedComplexOperations() {

	// Test case 1
	624@param#b.Img := NaN
	622@param#a.Img := -1.0
	621@param#a.Real := 1.0000000000000002
	623@param#b.Real := 1.000000000000002
    result := nestedComplexOperations(624@param#b.Img, 622@param#a.Img, 621@param#a.Real, 623@param#b.Real)
	if result != (-1.0000000000000018, -0.5000000000002273) {
	    t.Errorf("test_nestedComplexOperations1 failed")
	}
	
	// Test case 2
	624@param#b.Img := 1.0000000000002294
	622@param#a.Img := 1.0
	621@param#a.Real := 1.0000000000002294
	623@param#b.Real := 1.0000000000000002
    result := nestedComplexOperations(624@param#b.Img, 622@param#a.Img, 621@param#a.Real, 623@param#b.Real)
	if result != (1.0000000000000002, 1.0) {
	    t.Errorf("test_nestedComplexOperations2 failed")
	}
	
	// Test case 3
	624@param#b.Img := 1.000000000000256
	622@param#a.Img := 1.0000000000004539
	621@param#a.Real := -2.3182538441796384E-69
	623@param#b.Real := -0.0
    result := nestedComplexOperations(624@param#b.Img, 622@param#a.Img, 621@param#a.Real, 623@param#b.Real)
	if result != (-1.8051943758656497E-276, 1.0000000000004539) {
	    t.Errorf("test_nestedComplexOperations3 failed")
	}
	
	// Test case 4
	624@param#b.Img := 1.0
	622@param#a.Img := -1.0000000000004536
	621@param#a.Real := -1.0
	623@param#b.Real := -1.0000000000002274
    result := nestedComplexOperations(624@param#b.Img, 622@param#a.Img, 621@param#a.Real, 623@param#b.Real)
	if result != (1.0000000000002274, -1.0000000000004536) {
	    t.Errorf("test_nestedComplexOperations4 failed")
	}
	
}

func test_nestedConditions() {

	// Test case 1
	626@param#b := -1.0
	625@param#a := -1
    result := nestedConditions(626@param#b, 625@param#a)
	if result != 1.0000000000002276 {
	    t.Errorf("test_nestedConditions1 failed")
	}
	
	// Test case 2
	626@param#b := 1.0
	625@param#a := -2
    result := nestedConditions(626@param#b, 625@param#a)
	if result != 1.0 {
	    t.Errorf("test_nestedConditions2 failed")
	}
	
	// Test case 3
	626@param#b := NaN
	625@param#a := 0
    result := nestedConditions(626@param#b, 625@param#a)
	if result != NaN {
	    t.Errorf("test_nestedConditions3 failed")
	}
	
}

func test_pushPopIncrementality() {

	// Test case 1
	627@param#j := 1
    result := pushPopIncrementality(627@param#j)
	if result != 57 {
	    t.Errorf("test_pushPopIncrementality1 failed")
	}
	
	// Test case 2
	627@param#j := 0
    result := pushPopIncrementality(627@param#j)
	if result != 55 {
	    t.Errorf("test_pushPopIncrementality2 failed")
	}
	
}

func test_compareAndIncrement() {

	// Test case 1
	629@param#b := 0
	628@param#a := 9223372036854775807
    result := compareAndIncrement(629@param#b, 628@param#a)
	if result != -1 {
	    t.Errorf("test_compareAndIncrement1 failed")
	}
	
	// Test case 2
	629@param#b := 0
	628@param#a := 1
    result := compareAndIncrement(629@param#b, 628@param#a)
	if result != 1 {
	    t.Errorf("test_compareAndIncrement2 failed")
	}
	
	// Test case 3
	629@param#b := 0
	628@param#a := 0
    result := compareAndIncrement(629@param#b, 628@param#a)
	if result != 42 {
	    t.Errorf("test_compareAndIncrement3 failed")
	}
	
}

func test_complexOperations() {

	// Test case 1
	631@param#a.Img := -1.0
	630@param#a.Real := -1.0
	633@param#b.Img := 1.0
	632@param#b.Real := 1.0
    result := complexOperations(631@param#a.Img, 630@param#a.Real, 633@param#b.Img, 632@param#b.Real)
	if result != (1.0000000000002276, 1.0000000000002276) {
	    t.Errorf("test_complexOperations1 failed")
	}
	
	// Test case 2
	631@param#a.Img := -1.0
	630@param#a.Real := 4.0
	633@param#b.Img := -1.0000000000002272
	632@param#b.Real := 4.0
    result := complexOperations(631@param#a.Img, 630@param#a.Real, 633@param#b.Img, 632@param#b.Real)
	if result != (0.5, 1.0000000000002276) {
	    t.Errorf("test_complexOperations2 failed")
	}
	
	// Test case 3
	631@param#a.Img := -1.0
	630@param#a.Real := -1.0
	633@param#b.Img := -1.0000000000002276
	632@param#b.Real := -1.0000000000002276
    result := complexOperations(631@param#a.Img, 630@param#a.Real, 633@param#b.Img, 632@param#b.Real)
	if result != (-1.0, -1.0) {
	    t.Errorf("test_complexOperations3 failed")
	}
	
	// Test case 4
	631@param#a.Img := -1.0000000000002276
	630@param#a.Real := -1.0000000000002276
	633@param#b.Img := 1.0000000000000007
	632@param#b.Real := -1.0
    result := complexOperations(631@param#a.Img, 630@param#a.Real, 633@param#b.Img, 632@param#b.Real)
	if result != (-1.0, 1.0000000000000007) {
	    t.Errorf("test_complexOperations4 failed")
	}
	
}

func test_bitwiseOperations() {

	// Test case 1
	634@param#a := 1
	635@param#b := 1
    result := bitwiseOperations(634@param#a, 635@param#b)
	if result != 1 {
	    t.Errorf("test_bitwiseOperations1 failed")
	}
	
	// Test case 2
	634@param#a := 0
	635@param#b := 5
    result := bitwiseOperations(634@param#a, 635@param#b)
	if result != 5 {
	    t.Errorf("test_bitwiseOperations2 failed")
	}
	
	// Test case 3
	634@param#a := 50
	635@param#b := 0
    result := bitwiseOperations(634@param#a, 635@param#b)
	if result != 50 {
	    t.Errorf("test_bitwiseOperations3 failed")
	}
	
}

func test_floatOperations() {

	// Test case 1
	637@param#y := NaN
	636@param#x := NaN
    result := floatOperations(637@param#y, 636@param#x)
	if result != -1.0000000000002276 {
	    t.Errorf("test_floatOperations1 failed")
	}
	
	// Test case 2
	637@param#y := 1.0
	636@param#x := 1.0
    result := floatOperations(637@param#y, 636@param#x)
	if result != 1.0000000000002276 {
	    t.Errorf("test_floatOperations2 failed")
	}
	
	// Test case 3
	637@param#y := -1.0
	636@param#x := 1.0
    result := floatOperations(637@param#y, 636@param#x)
	if result != -1.0 {
	    t.Errorf("test_floatOperations3 failed")
	}
	
}

func main() {
	test_complexMagnitude()
	test_combinedBitwise()
	test_basicComplexOperations()
	test_compareElement()
	test_compareAge()
	test_nestedBitwise()
	test_complexComparison()
	test_advancedBitwise()
	test_integerOperations()
	test_mixedOperations()
	test_nestedComplexOperations()
	test_nestedConditions()
	test_pushPopIncrementality()
	test_compareAndIncrement()
	test_complexOperations()
	test_bitwiseOperations()
	test_floatOperations()
}

// Original code:

func compareElement(array []int, index int, value int) int {
	if index < 0 || index >= len(array) {
		return -1 // Индекс вне границ
	}
	element := array[index]
	if element > value {
		return 1 // Элемент больше
	} else if element < value {
		return -1 // Элемент меньше
	}
	return 0 // Элемент равен
}

type Person struct {
	Name string
	Age  int
}

func compareAge(people []*Person, index int, value int) int {
	if index < 0 || index >= len(people) {
		return -1 // Индекс вне границ
	}
	age := people[index].Age // Достаем возраст по индексу

	if age > value {
		return 1 // Возраст больше
	} else if age < value {
		return -1 // Возраст меньше
	}
	return 0 // Возраст равен
}

func basicComplexOperations(a complex128, b complex128) complex128 {
	if real(a) > real(b) {
		return a + b
	} else if imag(a) > imag(b) {
		return a - b
	}
	return a * b
}

func complexMagnitude(e complex128) float64 {
	magnitude := real(e)*real(e) + imag(e)*imag(e)
	return magnitude
}

func complexComparison(a complex128, b complex128) string {
	magA := complexMagnitude(a)
	magB := complexMagnitude(b)

	if magA > magB {
		return "Magnitude of a is greater than b"
	} else if magA < magB {
		return "Magnitude of b is greater than a"
	}
	return "Magnitudes are equal"
}

func complexOperations(a complex128, b complex128) complex128 {
	if real(a) == 0 && imag(a) == 0 {
		return b
	} else if real(b) == 0 && imag(b) == 0 {
		return a
	} else if real(a) > real(b) {
		return a / b
	}
	return a + b
}

func nestedComplexOperations(a complex128, b complex128) complex128 {
	if real(a) < 0 {
		if imag(a) < 0 {
			return a * b
		}
		return a + b
	}

	if imag(b) < 0 {
		return a - b
	}
	return a + b
}

func integerOperations(a int, b int) int {
	if a > b {
		return a + b
	} else if a < b {
		return a - b
	} else {
		return a * b
	}
}

func floatOperations(x float64, y float64) float64 {
	if x > y {
		return x / y
	} else if x < y {
		return x * y
	}
	return 0.0
}

func mixedOperations(a int, b float64) float64 {
	var result float64

	if a%2 == 0 {
		result = float64(a) + b
	} else {
		result = float64(a) - b
	}

	if result < 10 {
		result *= 2
	} else {
		result /= 2
	}

	return result
}

func nestedConditions(a int, b float64) float64 {
	if a < 0 {
		if b < 0 {
			return float64(a*-1) + b
		}
		return float64(a*-1) - b
	}
	return float64(a) + b
}

func bitwiseOperations(a int, b int) int {
	if a&1 == 0 && b&1 == 0 {
		return a | b
	} else if a&1 == 1 && b&1 == 1 {
		return a & b
	}
	return a ^ b
}

func advancedBitwise(a int, b int) int {
	if a > b {
		return a << 1
	} else if a < b {
		return b >> 1
	}
	return a ^ b
}

func combinedBitwise(a int, b int) int {
	if a&b == 0 {
		return a | b
	} else {
		result := a & b
		if result > 10 {
			return result ^ b
		}
		return result
	}
}

func nestedBitwise(a int, b int) int {
	if a < 0 {
		return -1
	}

	if b < 0 {
		return a ^ 0
	}

	if a&b == 0 {
		return a | b
	} else {
		return a & b
	}
}

func pushPopIncrementality(j int) int {
    result := j

    for i := 1; i <= 10; i++ {
        result += i
    }

    if result%2 == 0 {
        result++
    }

    return result
}

func compareAndIncrement(a, b int) int {
    if a > b {
        c := a + 1

        if (c > b) {
            return 1
        } else {
            return -1
        }
    }

    return 42
}

