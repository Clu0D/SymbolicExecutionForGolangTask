package main

import (
)

func test_InnerWhile() {

	// Test case 1
	639@param#a := 0
	638@param#l := 1
	640@param#border := 1
    result := InnerWhile(639@param#a, 638@param#l, 640@param#border)
	if result != 0 {
	    t.Errorf("test_InnerWhile1 failed")
	}
	
	// Test case 2
	638@param#l := 1
	639@param#a := 1
	640@param#border := 1
    result := InnerWhile(638@param#l, 639@param#a, 640@param#border)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_InnerWhile2 failed")
	}
	
}

func test_DivideByZeroCheckWithCycles() {

	// Test case 1
	642@param#n := 5
	641@param#l := 1
	643@param#x := -1
    result := DivideByZeroCheckWithCycles(642@param#n, 641@param#l, 643@param#x)
	if result != 1 {
	    t.Errorf("test_DivideByZeroCheckWithCycles1 failed")
	}
	
	// Test case 2
	642@param#n := 5
	641@param#l := 1
	643@param#x := 0
    result := DivideByZeroCheckWithCycles(642@param#n, 641@param#l, 643@param#x)
	if result != "make interface{} <- string ("Division by zero":string)" {
	    t.Errorf("test_DivideByZeroCheckWithCycles2 failed")
	}
	
	// Test case 3
	642@param#n := 0
	641@param#l := 1
	643@param#x := 1
    result := DivideByZeroCheckWithCycles(642@param#n, 641@param#l, 643@param#x)
	if result != "make interface{} <- string ("IllegalArgumentEx...":string)" {
	    t.Errorf("test_DivideByZeroCheckWithCycles3 failed")
	}
	
}

func test_ForCycleFour() {

	// Test case 1
	644@param#l := 1
	645@param#x := 15
    result := ForCycleFour(644@param#l, 645@param#x)
	if result != 1 {
	    t.Errorf("test_ForCycleFour1 failed")
	}
	
	// Test case 2
	645@param#x := 0
	644@param#l := 1
    result := ForCycleFour(645@param#x, 644@param#l)
	if result != -1 {
	    t.Errorf("test_ForCycleFour2 failed")
	}
	
}

func test_LoopWithConcreteBoundAndSymbolicBranching() {

	// Test case 1
	647@param#condition := false
	646@param#l := 1
    result := LoopWithConcreteBoundAndSymbolicBranching(647@param#condition, 646@param#l)
	if result != 0 {
	    t.Errorf("test_LoopWithConcreteBoundAndSymbolicBranching1 failed")
	}
	
	// Test case 2
	647@param#condition := true
	646@param#l := 1
    result := LoopWithConcreteBoundAndSymbolicBranching(647@param#condition, 646@param#l)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_LoopWithConcreteBoundAndSymbolicBranching2 failed")
	}
	
}

func test_MoveToException() {

	// Test case 1
	648@param#l := 1
	649@param#x := 401
    result := MoveToException(648@param#l, 649@param#x)
	if result != "make interface{} <- string ("IllegalArgumentEx...":string)" {
	    t.Errorf("test_MoveToException1 failed")
	}
	
	// Test case 2
	648@param#l := 1
	649@param#x := 399
    result := MoveToException(648@param#l, 649@param#x)
	if result != "make interface{} <- string ("IllegalArgumentEx...":string)" {
	    t.Errorf("test_MoveToException2 failed")
	}
	
}

func test_Factorial() {

	// Test case 1
	650@param#m := 1
	651@param#n := 1
    result := Factorial(650@param#m, 651@param#n)
	if result != 0, string!val!96784904 {
	    t.Errorf("test_Factorial1 failed")
	}
	
	// Test case 2
	650@param#m := 1
	651@param#n := -1
    result := Factorial(650@param#m, 651@param#n)
	if result != 0, string!val!-1565175336 {
	    t.Errorf("test_Factorial2 failed")
	}
	
}

func test_LoopWithSymbolicBound() {

	// Test case 1
	652@param#l := 1
	653@param#n := 1
    result := LoopWithSymbolicBound(652@param#l, 653@param#n)
	if result != 0 {
	    t.Errorf("test_LoopWithSymbolicBound1 failed")
	}
	
	// Test case 2
	652@param#l := 1
	653@param#n := 11
    result := LoopWithSymbolicBound(652@param#l, 653@param#n)
	if result != "make interface{} <- string ("Assumption violat...":string)" {
	    t.Errorf("test_LoopWithSymbolicBound2 failed")
	}
	
}

func test_TwoCondition() {

	// Test case 1
	655@param#x := 4
	654@param#c := 1
    result := TwoCondition(655@param#x, 654@param#c)
	if result != 1 {
	    t.Errorf("test_TwoCondition1 failed")
	}
	
	// Test case 2
	655@param#x := 1
	654@param#c := 1
    result := TwoCondition(655@param#x, 654@param#c)
	if result != 0 {
	    t.Errorf("test_TwoCondition2 failed")
	}
	
	// Test case 3
	655@param#x := 7
	654@param#c := 1
    result := TwoCondition(655@param#x, 654@param#c)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_TwoCondition3 failed")
	}
	
}

func test_LoopInsideLoop() {

	// Test case 1
	657@param#x := 12
	656@param#l := 1
    result := LoopInsideLoop(657@param#x, 656@param#l)
	if result != 1 {
	    t.Errorf("test_LoopInsideLoop1 failed")
	}
	
	// Test case 2
	656@param#l := 1
	657@param#x := 1
    result := LoopInsideLoop(656@param#l, 657@param#x)
	if result != 2 {
	    t.Errorf("test_LoopInsideLoop2 failed")
	}
	
	// Test case 3
	657@param#x := -9223372036854775804
	656@param#l := 1
    result := LoopInsideLoop(657@param#x, 656@param#l)
	if result != -1 {
	    t.Errorf("test_LoopInsideLoop3 failed")
	}
	
	// Test case 4
	656@param#l := 1
	657@param#x := 8
    result := LoopInsideLoop(656@param#l, 657@param#x)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_LoopInsideLoop4 failed")
	}
	
	// Test case 5
	657@param#x := 4611686018695823361
	656@param#l := 1
    result := LoopInsideLoop(657@param#x, 656@param#l)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_LoopInsideLoop5 failed")
	}
	
}

func test_CallInnerWhile() {

	// Test case 1
	659@param#value := 0
	658@param#l := 1
    result := CallInnerWhile(659@param#value, 658@param#l)
	if result != 0 {
	    t.Errorf("test_CallInnerWhile1 failed")
	}
	
}

func test_ForCycleFromJayHorn() {

	// Test case 1
	661@param#x := 0
	660@param#l := 1
    result := ForCycleFromJayHorn(661@param#x, 660@param#l)
	if result != 0 {
	    t.Errorf("test_ForCycleFromJayHorn1 failed")
	}
	
	// Test case 2
	661@param#x := 1
	660@param#l := 1
    result := ForCycleFromJayHorn(661@param#x, 660@param#l)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_ForCycleFromJayHorn2 failed")
	}
	
}

func test_StructureLoop() {

	// Test case 1
	663@param#x := 8
	662@param#l := 1
    result := StructureLoop(663@param#x, 662@param#l)
	if result != 1 {
	    t.Errorf("test_StructureLoop1 failed")
	}
	
	// Test case 2
	663@param#x := 0
	662@param#l := 1
    result := StructureLoop(663@param#x, 662@param#l)
	if result != -1 {
	    t.Errorf("test_StructureLoop2 failed")
	}
	
}

func test_Pow() {

	// Test case 1
	665@param#a := 0
	664@param#m := 1
	666@param#n := 2
    result := Pow(665@param#a, 664@param#m, 666@param#n)
	if result != 1, string!val!96784904 {
	    t.Errorf("test_Pow1 failed")
	}
	
	// Test case 2
	665@param#a := 0
	664@param#m := 1
	666@param#n := -1
    result := Pow(665@param#a, 664@param#m, 666@param#n)
	if result != 0, string!val!-1565175336 {
	    t.Errorf("test_Pow2 failed")
	}
	
}

func test_InnerLoop() {

	// Test case 1
	668@param#value := 0
	667@param#l := 1
    result := InnerLoop(668@param#value, 667@param#l)
	if result != 0 {
	    t.Errorf("test_InnerLoop1 failed")
	}
	
}

func test_LoopWithSymbolicBoundAndComplexControlFlow() {

	// Test case 1
	671@param#condition := true
	670@param#n := 2
	669@param#l := 1
    result := LoopWithSymbolicBoundAndComplexControlFlow(671@param#condition, 670@param#n, 669@param#l)
	if result != 0 {
	    t.Errorf("test_LoopWithSymbolicBoundAndComplexControlFlow1 failed")
	}
	
	// Test case 2
	671@param#condition := true
	669@param#l := 2
	670@param#n := 11
    result := LoopWithSymbolicBoundAndComplexControlFlow(671@param#condition, 669@param#l, 670@param#n)
	if result != "make interface{} <- string ("Assumption violat...":string)" {
	    t.Errorf("test_LoopWithSymbolicBoundAndComplexControlFlow2 failed")
	}
	
	// Test case 3
	671@param#condition := false
	670@param#n := 2
	669@param#l := 1
    result := LoopWithSymbolicBoundAndComplexControlFlow(671@param#condition, 670@param#n, 669@param#l)
	if result != 0 {
	    t.Errorf("test_LoopWithSymbolicBoundAndComplexControlFlow3 failed")
	}
	
}

func test_InfiniteRecursion() {

	// Test case 1
	672@param#m := 1
	673@param#i := 0
    result := InfiniteRecursion(672@param#m, 673@param#i)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_InfiniteRecursion1 failed")
	}
	
}

func test_LoopWithSymbolicBoundAndSymbolicBranching() {

	// Test case 1
	675@param#n := 2
	676@param#condition := true
	674@param#l := 1
    result := LoopWithSymbolicBoundAndSymbolicBranching(675@param#n, 676@param#condition, 674@param#l)
	if result != 0 {
	    t.Errorf("test_LoopWithSymbolicBoundAndSymbolicBranching1 failed")
	}
	
	// Test case 2
	675@param#n := 11
	676@param#condition := true
	674@param#l := 2
    result := LoopWithSymbolicBoundAndSymbolicBranching(675@param#n, 676@param#condition, 674@param#l)
	if result != "make interface{} <- string ("Assumption violat...":string)" {
	    t.Errorf("test_LoopWithSymbolicBoundAndSymbolicBranching2 failed")
	}
	
	// Test case 3
	675@param#n := 1
	676@param#condition := false
	674@param#l := 1
    result := LoopWithSymbolicBoundAndSymbolicBranching(675@param#n, 676@param#condition, 674@param#l)
	if result != 0 {
	    t.Errorf("test_LoopWithSymbolicBoundAndSymbolicBranching3 failed")
	}
	
}

func test_WhileCycle() {

	// Test case 1
	677@param#l := 1
	678@param#x := 0
    result := WhileCycle(677@param#l, 678@param#x)
	if result != 0 {
	    t.Errorf("test_WhileCycle1 failed")
	}
	
	// Test case 2
	677@param#l := 1
	678@param#x := 1
    result := WhileCycle(677@param#l, 678@param#x)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_WhileCycle2 failed")
	}
	
}

func test_Fib() {

	// Test case 1
	680@param#n := 2
	679@param#m := 1
    result := Fib(680@param#n, 679@param#m)
	if result != 2, string!val!96784904 {
	    t.Errorf("test_Fib1 failed")
	}
	
	// Test case 2
	680@param#n := -1
	679@param#m := 2
    result := Fib(680@param#n, 679@param#m)
	if result != 0, string!val!-1565175336 {
	    t.Errorf("test_Fib2 failed")
	}
	
}

func test_Sum() {

	// Test case 1
	681@param#m := 1
	683@param#snd := -1
    result := Sum(681@param#m, 683@param#snd)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_Sum1 failed")
	}
	
}

func test_FiniteCycle() {

	// Test case 1
	685@param#x := 0
	684@param#l := 1
    result := FiniteCycle(685@param#x, 684@param#l)
	if result != 0 {
	    t.Errorf("test_FiniteCycle1 failed")
	}
	
	// Test case 2
	685@param#x := 1
	684@param#l := 1
    result := FiniteCycle(685@param#x, 684@param#l)
	if result != "path stopped by interpreter" {
	    t.Errorf("test_FiniteCycle2 failed")
	}
	
}

func test_ForCycle() {

	// Test case 1
	686@param#l := 1
	687@param#x := 15
    result := ForCycle(686@param#l, 687@param#x)
	if result != 1 {
	    t.Errorf("test_ForCycle1 failed")
	}
	
	// Test case 2
	686@param#l := 1
	687@param#x := 0
    result := ForCycle(686@param#l, 687@param#x)
	if result != -1 {
	    t.Errorf("test_ForCycle2 failed")
	}
	
}

func test_LoopWithConcreteBound() {

	// Test case 1
	688@param#l := 1
	689@param#n := 0
    result := LoopWithConcreteBound(688@param#l, 689@param#n)
	if result != 45 {
	    t.Errorf("test_LoopWithConcreteBound1 failed")
	}
	
}

func main() {
	test_InnerWhile()
	test_DivideByZeroCheckWithCycles()
	test_ForCycleFour()
	test_LoopWithConcreteBoundAndSymbolicBranching()
	test_MoveToException()
	test_Factorial()
	test_LoopWithSymbolicBound()
	test_TwoCondition()
	test_LoopInsideLoop()
	test_CallInnerWhile()
	test_ForCycleFromJayHorn()
	test_StructureLoop()
	test_Pow()
	test_InnerLoop()
	test_LoopWithSymbolicBoundAndComplexControlFlow()
	test_InfiniteRecursion()
	test_LoopWithSymbolicBoundAndSymbolicBranching()
	test_WhileCycle()
	test_Fib()
	test_Sum()
	test_FiniteCycle()
	test_ForCycle()
	test_LoopWithConcreteBound()
}

// Original code:

type Loops struct{}

func (l *Loops) LoopWithConcreteBound(n int) int {
	result := 0
	for i := 0; i < 10; i++ {
		result += i
	}
	return result
}

func (l *Loops) LoopWithSymbolicBound(n int) int {
	if n > 10 {
		panic("Assumption violated: n should be less than or equal to 10")
	}

	result := 0
	for i := 0; i < n; i++ {
		result += i
	}
	return result
}

func (l *Loops) LoopWithConcreteBoundAndSymbolicBranching(condition bool) int {
	result := 0
	for i := 0; i < 10; i++ {
		if condition && i%2 == 0 {
			result += i
		}
	}
	return result
}

func (l *Loops) LoopWithSymbolicBoundAndSymbolicBranching(n int, condition bool) int {
	if n > 10 {
		panic("Assumption violated: n should be less than or equal to 10")
	}

	result := 0
	for i := 0; i < n; i++ {
		if condition && i%2 == 0 {
			result += i
		}
	}
	return result
}

func (l *Loops) LoopWithSymbolicBoundAndComplexControlFlow(n int, condition bool) int {
	if n > 10 {
		panic("Assumption violated: n should be less than or equal to 10")
	}

	result := 0
	for i := 0; i < n; i++ {
		if condition && i == 3 {
			break
		}
		if i%2 != 0 {
			continue
		}
		result += i
	}
	return result
}

func (l *Loops) ForCycle(x int) int {
	for i := 0; i < x; i++ {
		if i > 5 {
			return 1
		}
	}
	return -1
}

func (l *Loops) ForCycleFour(x int) int {
	for i := 0; i < x; i++ {
		if i > 4 {
			return 1
		}
	}
	return -1
}

func (l *Loops) FiniteCycle(x int) int {
	for {
		if x%519 == 0 {
			break
		} else {
			x++
		}
	}
	return x
}

func (l *Loops) ForCycleFromJayHorn(x int) int {
	r := 0
	for i := 0; i < x; i++ {
		r += 2
	}
	return r
}

func (l *Loops) DivideByZeroCheckWithCycles(n int, x int) int {
	if n < 5 {
		panic("IllegalArgumentException: n < 5")
	}
	j := 0
	for i := 0; i < n; i++ {
		j += i
	}
	if x == 0 {
		panic("Division by zero")
	}
	j /= x
	for i := 0; i < n; i++ {
		j += i
	}
	return 1
}

func (l *Loops) MoveToException(x int) {
	if x < 400 {
		for i := x; i < 400; i++ {
			x++
		}
	}

	if x > 400 {
		for i := x; i > 400; i-- {
			x--
		}
	}

	if x == 400 {
		panic("IllegalArgumentException")
	}
}

func (l *Loops) WhileCycle(x int) int {
	i := 0
	sum := 0
	for i < x {
		sum += i
		i++
	}
	return sum
}

func (l *Loops) CallInnerWhile(value int) int {
	return l.InnerWhile(value, 42)
}

func (l *Loops) InnerLoop(value int) int {
	cycleDependedCondition := &CycleDependedCondition{}
	return cycleDependedCondition.TwoCondition(value)
}

func (l *Loops) InnerWhile(a int, border int) int {
	res := a
	for res >= border {
		res -= border
	}
	return res
}

func (l *Loops) LoopInsideLoop(x int) int {
	for i := x - 5; i < x; i++ {
		if i < 0 {
			return 2
		} else {
			for j := i; j < x+i; j++ {
				if j == 7 {
					return 1
				}
			}
		}
	}
	return -1
}

func (l *Loops) StructureLoop(x int) int {
	for i := 0; i < x; i++ {
		if i == 2 {
			return 1
		}
	}
	return -1
}

type CycleDependedCondition struct{}

func (c *CycleDependedCondition) TwoCondition(x int) int {
	for i := 0; i < x; i++ {
		if i > 2 && x == 4 {
			return 1
		}
	}
	return 0
}


type MathExamples struct{}

func (m *MathExamples) Factorial(n int) (int, error) {
	if n < 0 {
		return 0, errors.New("IllegalArgumentException")
	}
	if n == 0 {
		return 1, nil
	}
	result, _ := m.Factorial(n - 1)
	return n * result, nil
}

func (m *MathExamples) Fib(n int) (int, error) {
	if n < 0 {
		return 0, errors.New("IllegalArgumentException")
	}
	if n == 0 {
		return 0, nil
	}
	if n == 1 {
		return 1, nil
	}
	fib1, _ := m.Fib(n - 1)
	fib2, _ := m.Fib(n - 2)
	return fib1 + fib2, nil
}

func (m *MathExamples) Sum(fst, snd int) int {
	if snd == 0 {
		return fst
	}
	sign := int(float64(snd) / float64(abs(snd)))
	return m.Sum(fst+sign, snd-sign)
}

func (m *MathExamples) Pow(a, n int) (int, error) {
	if n < 0 {
		return 0, errors.New("IllegalArgumentException")
	}
	if n == 0 {
		return 1, nil
	}
	if n%2 == 1 {
		result, _ := m.Pow(a, n-1)
		return result * a, nil
	} else {
		b, _ := m.Pow(a, n/2)
		return b * b, nil
	}
}

func (m *MathExamples) InfiniteRecursion(i int) {
	m.InfiniteRecursion(i + 1)
}

func abs(x int) int {
	if x < 0 {
		return -x
	}
	return x
}

