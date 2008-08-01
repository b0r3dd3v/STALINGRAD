(MODULE STALINGRADLIB-STUFF)
;;; LaHaShem HaAretz U'Mloah
;;; $Id$

;;; Stalingrad 0.1 - AD for VLAD, a functional language.
;;; Copyright 2004, 2005, 2006, 2007, and 2008 Purdue University. All rights
;;; reserved.

;;; This program is free software; you can redistribute it and/or
;;; modify it under the terms of the GNU General Public License
;;; as published by the Free Software Foundation; either version 2
;;; of the License, or (at your option) any later version.

;;; This program is distributed in the hope that it will be useful,
;;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;;; GNU General Public License for more details.

;;; You should have received a copy of the GNU General Public License
;;; along with this program; if not, write to the Free Software
;;; Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

;;; written by:
;;;    Jeffrey Mark Siskind
;;;    School of Electrical and Computer Engineering
;;;    Purdue University
;;;    465 Northwestern Avenue
;;;    Lafayette IN 47907-2035 USA
;;;    voice: +1 765 496-3197
;;;    FAX:   +1 765 494-6440
;;;    qobi@purdue.edu
;;;    ftp://ftp.ecn.purdue.edu/qobi
;;;    http://www.ece.purdue.edu/~qobi
;;;             and
;;;    Barak A. Pearlmutter
;;;    Hamilton Institute
;;;    National University of Ireland Maynooth
;;;    Co. Kildare
;;;    Ireland
;;;    voice: +353 1 7086100
;;;    FAX:   +353 1 7086269
;;;    barak@cs.nuim.ie
;;;    http://www.bcl.hamilton.ie/~barak/

(include "QobiScheme.sch")
(include "c-externals.sc")
(include "stalingradlib-stuff.sch")

;;; needs work
;;;  1. zero, perturb, unperturb, primal, tangent, bundle, sensitize,
;;;     unsensitize, plus, *j, and *j-inverse should be lazy.
;;;  2. Really need to get rid of anonymous gensyms to get read/write
;;;     invariance.
;;;  3. unary -
;;;  4. begin, case, delay, do, named let, quasiquote, unquote,
;;;     unquote-splicing, internal defines
;;;  5. chars, ports, eof object, symbols, continuations, strings, vectors

;;; Key
;;;  e: concrete or abstract expression
;;;  p: concrete or abstract parameter
;;;  x: concrete or abstract variable
;;;  b: concrete syntactic, variable, or value binding
;;;  v: concrete or abstract value
;;;  d: definition
;;;  record, gensym, result, free-variables, message, callee, argument,
;;;  procedure

;;; Macros

(define-macro assert
 (lambda (form expander)
  (unless (= (length form) 2)
   (error 'assert "Wrong number of arguments: ~s" form))
  (expander `(assert-internal (lambda () ,(second form))) expander)))

(define-macro time-it
 ;; belongs in QobiScheme
 (lambda (form expander)
  (let* ((string (format #f "~a" (second form)))
	 (string (if (<= (string-length string) 60)
		     string
		     (substring string 0 60))))
   (expander
    (if #f				;debugging
	`(time ,(format #f "~~a ~a~~%" string) (lambda () ,(second form)))
	(second form))
    expander))))

(define-macro time-it-bucket
 ;; belongs in QobiScheme
 (lambda (form expander)
  (expander
   (if #f				;debugging
       `(time-bucket ,(second form) (lambda () ,(third form)))
       (third form))
   expander)))

;;; Structures

(define-structure variable
 name
 index
 perturbationify
 forwardify
 sensitivityify
 reverseify
 unperturbationify
 unforwardify
 unsensitivityify
 unreverseify)

(define-structure constant-expression
 parents
 environment-bindings
 value)

(define-structure variable-access-expression
 parents
 environment-bindings
 variable)

(define-structure lambda-expression
 alpha-conversion-inverse
 perturbation-transform
 perturbation-transform-inverse
 forward-transform
 forward-transform-inverse
 sensitivity-transform
 sensitivity-transform-inverse
 reverse-transform
 reverse-transform-inverse
 nonrecursive-closures
 recursive-closures
 parents
 environment-bindings
 free-variables
 parameter
 body)

(define-structure application
 parents
 environment-bindings
 enqueue?
 free-variables
 callee
 argument
 callee-indices
 argument-indices)

(define-structure letrec-expression
 parents
 environment-bindings
 enqueue?
 free-variables
 procedure-variables
 lambda-expressions
 body
 indices)

(define-structure cons-expression
 parents
 environment-bindings
 enqueue?
 free-variables
 tags
 car
 cdr
 car-indices
 cdr-indices)

(define-structure variable-binding variable expression)

(define-structure parameter-binding parameter expression)

(define-structure value-binding variable value)

(define-structure alpha-binding variable1 variable2)

(define-structure primitive-procedure
 name concrete abstract symbolic generator forward reverse meter)

(define-structure nonrecursive-closure
 values
 lambda-expression
 canonize-cache
 intern-cache
 zero-cache
 perturb-cache
 unperturb-cache
 primal-cache
 tangent-cache
 sensitize-cache
 unsensitize-cache
 *j-cache
 *j-inverse-cache
 widen
 c:index
 boxed?
 void?
 inline-zero?
 inline-perturb?
 inline-unperturb?
 inline-primal?
 inline-tangent?
 inline-sensitize?
 inline-unsensitize?
 inline-*j?
 inline-*j-inverse?)

(define-structure recursive-closure
 values
 procedure-variables			;vector
 lambda-expressions			;vector
 index
 canonize-cache
 intern-cache
 zero-cache
 perturb-cache
 unperturb-cache
 primal-cache
 tangent-cache
 sensitize-cache
 unsensitize-cache
 *j-cache
 *j-inverse-cache
 widen
 c:index
 boxed?
 void?
 inline-zero?
 inline-perturb?
 inline-unperturb?
 inline-primal?
 inline-tangent?
 inline-sensitize?
 inline-unsensitize?
 inline-*j?
 inline-*j-inverse?)

(define-structure perturbation-tagged-value
 primal
 canonize-cache
 intern-cache
 zero-cache
 perturb-cache
 sensitize-cache
 *j-cache
 widen
 c:index
 boxed?
 void?
 inline-zero?
 inline-perturb?
 inline-unperturb?
 inline-primal?
 inline-tangent?
 inline-sensitize?
 inline-unsensitize?
 inline-*j?
 inline-*j-inverse?)

(define-structure bundle
 primal
 tangent
 canonize-cache
 intern-cache
 zero-cache
 perturb-cache
 sensitize-cache
 *j-cache
 widen
 c:index
 boxed?
 void?
 inline-zero?
 inline-perturb?
 inline-unperturb?
 inline-primal?
 inline-tangent?
 inline-sensitize?
 inline-unsensitize?
 inline-*j?
 inline-*j-inverse?)

(define-structure sensitivity-tagged-value
 primal
 canonize-cache
 intern-cache
 zero-cache
 perturb-cache
 sensitize-cache
 *j-cache
 widen
 c:index
 boxed?
 void?
 inline-zero?
 inline-perturb?
 inline-unperturb?
 inline-primal?
 inline-tangent?
 inline-sensitize?
 inline-unsensitize?
 inline-*j?
 inline-*j-inverse?)

(define-structure reverse-tagged-value
 primal
 canonize-cache
 intern-cache
 zero-cache
 perturb-cache
 sensitize-cache
 *j-cache
 widen
 c:index
 boxed?
 void?
 inline-zero?
 inline-perturb?
 inline-unperturb?
 inline-primal?
 inline-tangent?
 inline-sensitize?
 inline-unsensitize?
 inline-*j?
 inline-*j-inverse?)

(define-structure tagged-pair
 tags
 car
 cdr
 canonize-cache
 intern-cache
 zero-cache
 perturb-cache
 unperturb-cache
 primal-cache
 tangent-cache
 bundle-cache
 sensitize-cache
 unsensitize-cache
 plus-cache
 *j-cache
 *j-inverse-cache
 widen
 c:index
 boxed?
 void?
 inline-zero?
 inline-perturb?
 inline-unperturb?
 inline-primal?
 inline-tangent?
 inline-bundle?
 inline-sensitize?
 inline-unsensitize?
 inline-plus?
 inline-*j?
 inline-*j-inverse?)

(define-structure union
 values
 canonize-cache
 intern-cache
 zero-cache
 perturb-cache
 unperturb-cache
 primal-cache
 tangent-cache
 bundle-cache
 sensitize-cache
 unsensitize-cache
 plus-cache
 *j-cache
 *j-inverse-cache
 widen
 c:index
 boxed?
 void?
 inline-zero?
 inline-perturb?
 inline-unperturb?
 inline-primal?
 inline-tangent?
 inline-bundle?
 inline-sensitize?
 inline-unsensitize?
 inline-plus?
 inline-*j?
 inline-*j-inverse?
 ;; needs work: This is ambiguous with the other notion of tag.
 tag)

(define-structure environment-binding values value)

(define-structure if-instance v)

(define-structure function-instance v1 v2 inline?)

(define-structure widener-instance v1 v2)

(define-structure name-unit abstract-value code)

(define-structure call-unit abstract-value code xs)

(define-structure panic-unit x)

(define-structure +-unit x y)

(define-structure --unit x y)

(define-structure *-unit x y)

(define-structure /-unit x y)

(define-structure sqrt-unit x)

(define-structure exp-unit x)

(define-structure log-unit x)

(define-structure sin-unit x)

(define-structure cos-unit x)

(define-structure atan-unit x y)

(define-structure =-unit x y)

(define-structure <-unit x y)

(define-structure >-unit x y)

(define-structure <=-unit x y)

(define-structure >=-unit x y)

(define-structure zero?-unit x)

(define-structure positive?-unit x)

(define-structure negative?-unit x)

(define-structure read-real-unit x)

(define-structure write-real-unit x)

;;; Variables

(define *time-buckets* #f)		; belongs in QobiScheme

(define *gensym* 0)

(define *alpha* 0)

(define *value-bindings* '())

(define *stack* '())

(define *trace-level* 0)

(define *error* #f)

(define *error?* #f)

(define *mode* 'concrete)

(define *with-concrete?* #f)

(define *variables* '())

(define *backpropagator-variables* (vector #f))

(define *anf-variables* (vector #f))

(define *expressions* '())

(define *queue* '())

(define *frozen?* #f)			;means cannot intern

;;; Can better index the following six.

(define *perturbation-tagged-values* '())

(define *bundles* '())

(define *sensitivity-tagged-values* '())

(define *reverse-tagged-values* '())

(define *tagged-pairs* '())

(define *unions* '())

(define *empty-abstract-value* #f)

(define *abstract-boolean* #f)

;;; Can better index the following.

(define *c:indices* '())

(define *units* '())

;;; Parameters

(define *include-path* '())

(define *assert?* #f)

(define *wizard?* #f)

(define *flow-analysis?* #f)

(define *compile?* #f)

(define *metered?* #f)

(define *trace-primitive-procedures?* #f)

(define *trace-nonrecursive-closures?* #f)

(define *trace-recursive-closures?* #f)

(define *trace-argument/result?* #f)

(define *unabbreviate-executably?* #f)

(define *unabbreviate-transformed?* #f)

(define *unabbreviate-nonrecursive-closures?* #f)

(define *unabbreviate-recursive-closures?* #f)

(define *pp?* #f)

(define *verbose* #f)

(define *imprecise-inexacts?* #f)

(define *warnings?* #f)

(define *real-width-limit* #f)

(define *closure-width-limit* #f)

(define *perturbation-tagged-value-width-limit* #f)

(define *bundle-width-limit* #f)

(define *sensitivity-tagged-value-width-limit* #f)

(define *reverse-tagged-value-width-limit* #f)

(define *tagged-pair-width-limit* #f)

(define *closure-depth-limit* #f)

(define *backpropagator-depth-limit* #f)

(define *perturbation-tagged-value-depth-limit* #f)

(define *bundle-depth-limit* #f)

(define *sensitivity-tagged-value-depth-limit* #f)

(define *reverse-tagged-value-depth-limit* #f)

(define *tagged-pair-depth-limit* #f)

(define *order-limit* #f)

(define *widen-lists?* #f)

(define *almost-union-free?* #f)

(define *canonized?* #f)

(define *interned?* #f)

(define *expensive-checks?* #f)

(define *union* "struct")

;;; Procedures

;;; General

(define (initialize-time-buckets n)
 ;; belongs in QobiScheme
 (set! *time-buckets* (make-vector n 0)))

(define (time-bucket i thunk)
 ;; belongs in QobiScheme
 (let* ((start (clock-sample))
	(result (thunk))
	(end (clock-sample)))
  (vector-set!
   *time-buckets* i (+ (vector-ref *time-buckets* i) (- end start)))
  result))

(define (print-time-buckets)
 ;; belongs in QobiScheme
 (for-each-n (lambda (i)
	      (format #t "~a ~a~%"
		      (number->string-of-length i 4)
		      (number->string-of-length-and-precision
		       (vector-ref *time-buckets* i) 8 2)))
	     (vector-length *time-buckets*)))

(define (duplicatesp? p xs)
 ;; belongs in QobiScheme
 (and (not (null? xs))
      (or (memp p (first xs) (rest xs)) (duplicatesp? p (rest xs)))))

(define (positionp-vector p x v)
 ;; belongs in QobiScheme
 (let loop ((i 0))
  (cond ((>= i (vector-length v)) #f)
	((p x (vector-ref v i)) i)
	(else (loop (+ i 1))))))

(define (map-reduce g i f l . ls)
 ;; belongs in QobiScheme
 (let loop ((l (reverse l)) (ls (map reverse ls)) (c i))
  (if (null? l)
      c
      (loop (rest l) (map rest ls) (g (apply f (first l) (map first ls)) c)))))

(define (rest* l k) (if (zero? k) l (rest* (rest l) (- k 1))))

(define (maximal-elements <=? s)
 ;; belongs in QobiScheme
 (remove-if
  (lambda (e)
   (some (lambda (e-prime) (and (not (eq? e-prime e)) (<=? e e-prime))) s))
  s))

(define (cross-product f l1 l2)
 (map-reduce append '() (lambda (x1) (map (lambda (x2) (f x1 x2)) l2)) l1))

(define (with-concrete thunk)
 ;; needs work: To disable errors.
 (let ((mode *mode*) (with-concrete? *with-concrete?*))
  (set! *mode* 'concrete)
  (set! *with-concrete?* #t)
  (let ((result (thunk)))
   (set! *mode* mode)
   (set! *with-concrete?* with-concrete?)
   result)))

(define (with-abstract thunk)
 (let ((mode *mode*) (canonized? *canonized?*) (interned? *interned?*))
  (set! *mode* 'abstract)
  (set! *canonized?* #t)
  (set! *interned?* #t)
  (let ((result (thunk)))
   (set! *mode* mode)
   (set! *canonized?* canonized?)
   (set! *interned?* interned?)
   result)))

(define (with-symbolic thunk)
 (let ((mode *mode*) (canonized? *canonized?*) (interned? *interned?*))
  (set! *mode* 'symbolic)
  (set! *canonized?* #f)
  (set! *interned?* #f)
  (let ((result (thunk)))
   (set! *mode* mode)
   (set! *canonized?* canonized?)
   (set! *interned?* interned?)
   result)))

(define (without-warnings thunk)
 (let ((warnings? *warnings?*))
  (set! *warnings?* #f)
  (let ((result (thunk)))
   (set! *warnings?* warnings?)
   result)))

(define (some-cps p l cs k)
 (if (null? l)
     (k #f cs)
     (p (first l)
	cs
	(lambda (r? cs) (if r? (k #t cs) (some-cps p (rest l) cs k))))))

(define (every-cps p l cs k)
 (if (null? l)
     (k #t cs)
     (p (first l)
	cs
	(lambda (r? cs) (if r? (every-cps p (rest l) cs k) (k #f cs))))))

(define (every2-cps p l1 l2 cs k)
 (if (null? l1)
     (k #t cs)
     (p (first l1)
	(first l2)
	cs
	(lambda (r? cs)
	 (if r? (every2-cps p (rest l1) (rest l2) cs k) (k #f cs))))))

(define (map-cps p l cs k)
 (let loop ((l l) (cs cs) (n '()))
  (if (null? l)
      (k (reverse n) cs)
      (p (first l) cs (lambda (r cs) (loop (rest l) cs (cons r n)))))))

(define (map-cps-non-cs p l k)
 (let loop ((l l) (n '()))
  (if (null? l)
      (k (reverse n))
      (p (first l) (lambda (r) (loop (rest l) (cons r n)))))))

(define (map2-cps p l1 l2 cs k)
 (let loop ((l1 l1) (l2 l2) (cs cs) (n '()))
  (if (null? l1)
      (k (reverse n) cs)
      (p (first l1)
	 (first l2)
	 cs
	 (lambda (r cs) (loop (rest l1) (rest l2) cs (cons r n)))))))

(define (map2-cps-non-cs p l1 l2 k)
 (let loop ((l1 l1) (l2 l2) (n '()))
  (if (null? l1)
      (k (reverse n))
      (p (first l1)
	 (first l2)
	 (lambda (r) (loop (rest l1) (rest l2) (cons r n)))))))

(define (reduce-cps p l i cs k)
 (let loop ((l l) (cs cs) (i i))
  (if (null? l)
      (k i cs)
      (p i (first l) cs (lambda (i cs) (loop (rest l) cs i))))))

;;; Error Handing

(define (internal-error . arguments)
 (if (null? arguments)
     (panic "Internal error")
     (apply panic
	    (format #f "Internal error: ~a" (first arguments))
	    (rest arguments))))

(define (assert-internal thunk)
 (when *assert?* (unless (thunk) (internal-error))))

(define (unimplemented . arguments)
 (if (null? arguments)
     (panic "Not (yet) implemented")
     (apply panic
	    (format #f "Not (yet) implemented: ~a" (first arguments))
	    (rest arguments))))

(define (compile-time-error message . arguments)
 (apply format stderr-port message arguments)
 (newline stderr-port)
 (exit -1))

(define (compile-time-warning message . vs)
 (assert (eq? *mode* 'abstract))
 (when *warnings?*
  (without-warnings
   (lambda ()
    (for-each (lambda (v)
	       ((if *pp?* pp write) (externalize v) stderr-port)
	       (newline stderr-port))
	      vs)))
  (display "Warning: " stderr-port)
  (display message stderr-port)
  (newline stderr-port))
 (empty-abstract-value))

(define (run-time-warning message . vs)
 (assert (eq? *mode* 'concrete))
 (when *warnings?*
  (when *error?*
   (display "Nested warning: " stderr-port)
   (display message stderr-port)
   (newline stderr-port)
   (display "Error: " stderr-port)
   (display *error* stderr-port)
   (newline stderr-port)
   (exit -1))
  (set! *error* message)
  (set! *error?* #t)
  (without-warnings
   (lambda ()
    (unless *with-concrete?*
     (format stderr-port "Stack trace~%")
     (for-each (lambda (record)
		(display "Procedure: " stderr-port)
		((if *pp?* pp write) (externalize (first record)) stderr-port)
		(newline stderr-port)
		(display "Argument: " stderr-port)
		((if *pp?* pp write) (externalize (second record)) stderr-port)
		(newline stderr-port)
		(newline stderr-port))
	       *stack*)
     (newline stderr-port))
    (for-each (lambda (v)
	       ((if *pp?* pp write) (externalize v) stderr-port)
	       (newline stderr-port))
	      vs)))
  (display "Warning: " stderr-port)
  (display message stderr-port)
  (newline stderr-port)
  (set! *error?* #f)))

(define (run-time-error message . vs)
 (assert (eq? *mode* 'concrete))
 (when *error?*
  (display "Nested error: " stderr-port)
  (display message stderr-port)
  (newline stderr-port)
  (display "Error: " stderr-port)
  (display *error* stderr-port)
  (newline stderr-port)
  (exit -1))
 (set! *error* message)
 (set! *error?* #t)
 (without-warnings
  (lambda ()
   (unless *with-concrete?*
    (format stderr-port "Stack trace~%")
    (for-each (lambda (record)
	       (display "Procedure: " stderr-port)
	       ((if *pp?* pp write) (externalize (first record)) stderr-port)
	       (newline stderr-port)
	       (display "Argument: " stderr-port)
	       ((if *pp?* pp write) (externalize (second record)) stderr-port)
	       (newline stderr-port)
	       (newline stderr-port))
	      *stack*)
    (newline stderr-port))
   (for-each (lambda (v)
	      ((if *pp?* pp write) (externalize v) stderr-port)
	      (newline stderr-port))
	     vs)))
 (display "Error: " stderr-port)
 (display message stderr-port)
 (newline stderr-port)
 (exit -1))

(define (ad-warning message . vs)
 (case *mode*
  ((concrete) (apply run-time-warning message vs))
  ((abstract) (apply compile-time-warning message vs))
  ((symbolic) #f)
  (else (internal-error))))

(define (ad-error message . vs)
 (case *mode*
  ((concrete) (apply run-time-error (format #f message "is not") vs))
  ((abstract)
   (apply compile-time-warning (format #f message "might not be") vs))
  ((symbolic) (new-panic-unit (format #f message "is not")))
  (else (internal-error))))

;;; Tags

(define (empty-tags) '())

(define (empty-tags? tags) (or (eq? tags 'unknown) (null? tags)))

(define (add-tag tag tags) (cons tag tags))

(define (tagged? tag tags)
 (or (eq? tags 'unknown)
     (and (not (empty-tags? tags)) (eq? (first tags) tag))))

(define (remove-tag tag tags)
 (assert (tagged? tag tags))
 (if (eq? tags 'unknown) tags (rest tags)))

(define (prefix-tags? tags1 tags2)
 (or (eq? tags1 'unknown)
     (eq? tags2 'unknown)
     (empty-tags? tags1)
     (and (not (empty-tags? tags1))
	  (not (empty-tags? tags2))
	  (eq? (first tags1) (first tags2))
	  (prefix-tags? (rest tags1) (rest tags2)))))

(define (equal-tags? tags1 tags2)
 (or (eq? tags1 'unknown)
     (eq? tags2 'unknown)
     (and (empty-tags? tags1) (empty-tags? tags2))
     (and (not (empty-tags? tags1))
	  (not (empty-tags? tags2))
	  (eq? (first tags1) (first tags2))
	  (equal-tags? (rest tags1) (rest tags2)))))

;;; Variables

(define (gensym)
 (let ((gensym *gensym*))
  (set! *gensym* (+ *gensym* 1))
  (string->uninterned-symbol
   (format #f "G~a" (number->padded-string-of-length gensym 9)))))

(define (concrete-user-variable? x)
 (and (symbol? x)
      (not (memq x '(quote
		     lambda
		     letrec
		     let
		     let*
		     if
		     cons
		     cons*
		     list
		     cond
		     else
		     and
		     or
		     alpha
		     anf
		     backpropagator
		     perturbation
		     forward
		     sensitivity
		     reverse)))))

(define (concrete-variable? x)
 (or (concrete-user-variable? x)
     (and (list? x)
	  (= (length x) 3)
	  (eq? (first x) 'alpha)
	  (concrete-variable? (second x))
	  (integer? (third x))
	  (exact? (third x))
	  (not (negative? (third x))))
     (and (list? x)
	  (= (length x) 2)
	  (eq? (first x) 'anf)
	  (integer? (second x))
	  (exact? (second x))
	  (not (negative? (second x))))
     (and (list? x)
	  (= (length x) 2)
	  (eq? (first x) 'backpropagator)
	  (integer? (second x))
	  (exact? (second x))
	  (not (negative? (second x))))
     (and (list? x)
	  (= (length x) 2)
	  (eq? (first x) 'perturbation)
	  (concrete-variable? (second x)))
     (and (list? x)
	  (= (length x) 2)
	  (eq? (first x) 'forward)
	  (concrete-variable? (second x)))
     (and (list? x)
	  (= (length x) 2)
	  (eq? (first x) 'sensitivity)
	  (concrete-variable? (second x)))
     (and (list? x)
	  (= (length x) 2)
	  (eq? (first x) 'reverse)
	  (concrete-variable? (second x)))))

(define (variable-anf-max x)
 (let loop ((x (variable-name x)))
  (cond ((symbol? x) 0)
	((list? x)
	 (case (first x)
	  ((alpha) (loop (second x)))
	  ((anf) (second x))
	  ((backpropagator) 0)
	  ((perturbation forward sensitivity reverse) (loop (second x)))
	  (else (internal-error))))
	(else (internal-error)))))

(define (concrete-variable=? x1 x2)
 (assert (and (concrete-variable? x1) (concrete-variable? x2)))
 (equal? x1 x2))

(define (variable=? x1 x2)
 (assert (and (variable? x1) (variable? x2)))
 (eq? x1 x2))

(define (concrete-variable-base x)
 (if (and (list? x) (eq? (first x) 'alpha))
     (concrete-variable-base (second x))
     x))

(define (concrete-variable-alpha x)
 (if (and (list? x) (eq? (first x) 'alpha))
     (cons (third x) (concrete-variable-alpha (second x)))
     '()))

(define (base-concrete-variable<? x1 x2)
 (if (symbol? x1)
     (if (symbol? x2)
	 (string<? (symbol->string x1) (symbol->string x2))
	 #t)
     (if (symbol? x2)
	 #f
	 (if (eq? (first x1) (first x2))
	     (case (first x1)
	      ((anf backpropagator) (< (second x1) (second x2)))
	      ((perturbation forward sensitivity reverse)
	       (concrete-variable<? (second x1) (second x2)))
	      (else (internal-error)))
	     (not (not (memq (first x2)
			     (memq (first x1)
				   '(anf
				     backpropagator
				     perturbation
				     forward
				     sensitivity
				     reverse)))))))))

(define (concrete-variable<? x1 x2)
 (or (base-concrete-variable<? (concrete-variable-base x1)
			       (concrete-variable-base x2))
     (and (concrete-variable=? (concrete-variable-base x1)
			       (concrete-variable-base x2))
	  ((lexicographically<? < =)
	   (reverse (concrete-variable-alpha x1))
	   (reverse (concrete-variable-alpha x2))))))

(define (variable<? x1 x2)
 (concrete-variable<? (variable-name x1) (variable-name x2)))

(define (sort-variables xs) (sort xs variable<? identity))

(define (new-variable x)
 (assert (concrete-variable? x))
 (or (find-if (lambda (x0) (concrete-variable=? (variable-name x0) x))
	      *variables*)
     (let ((x0 (make-variable x #f #f #f #f #f #f #f #f #f)))
      (set! *variables* (cons x0 *variables*))
      x0)))

(define (anfify i)
 (if (< i (vector-length *anf-variables*))
     (or (vector-ref *anf-variables* i)
	 (let ((x (new-variable `(anf ,i))))
	  (vector-set! *anf-variables* i x)
	  x))
     (let ((anf-variables
	    (make-vector (* 2 (vector-length *anf-variables*)) #f))
	   (x (new-variable `(anf ,i))))
      (for-each-n
       (lambda (i)
	(vector-set! anf-variables i (vector-ref *anf-variables* i)))
       (vector-length *anf-variables*))
      (set! *anf-variables* anf-variables)
      (vector-set! *anf-variables* i x)
      x)))

(define (backpropagatorify i)
 (if (< i (vector-length *backpropagator-variables*))
     (or (vector-ref *backpropagator-variables* i)
	 (let ((x (new-variable `(backpropagator ,i))))
	  (vector-set! *backpropagator-variables* i x)
	  x))
     (let ((backpropagator-variables
	    (make-vector (* 2 (vector-length *backpropagator-variables*)) #f))
	   (x (new-variable `(backpropagator ,i))))
      (for-each-n
       (lambda (i)
	(vector-set!
	 backpropagator-variables i (vector-ref *backpropagator-variables* i)))
       (vector-length *backpropagator-variables*))
      (set! *backpropagator-variables* backpropagator-variables)
      (vector-set! *backpropagator-variables* i x)
      x)))

(define (variable-order x)
 (max (if (variable-unperturbationify x)
	  (+ (variable-order (variable-unperturbationify x)) 1)
	  0)
      (if (variable-unforwardify x)
	  (+ (variable-order (variable-unforwardify x)) 1)
	  0)
      (if (variable-unsensitivityify x)
	  (+ (variable-order (variable-unsensitivityify x)) 1)
	  0)
      (if (variable-unreverseify x)
	  (+ (variable-order (variable-unreverseify x)) 1)
	  0)))

(define (perturbationify x)
 (or (variable-perturbationify x)
     (let ((x0 (new-variable `(perturbation ,(variable-name x)))))
      (when (and *order-limit* (>= (variable-order x) *order-limit*))
       (compile-time-error "Order limit exceeded"))
      (set-variable-perturbationify! x x0)
      (set-variable-unperturbationify! x0 x)
      x0)))

(define (forwardify x)
 (or (variable-forwardify x)
     (let ((x0 (new-variable `(forward ,(variable-name x)))))
      (when (and *order-limit* (>= (variable-order x) *order-limit*))
       (compile-time-error "Order limit exceeded"))
      (set-variable-forwardify! x x0)
      (set-variable-unforwardify! x0 x)
      x0)))

(define (sensitivityify x)
 (or (variable-sensitivityify x)
     (let ((x0 (new-variable `(sensitivity ,(variable-name x)))))
      (when (and *order-limit* (>= (variable-order x) *order-limit*))
       (compile-time-error "Order limit exceeded"))
      (set-variable-sensitivityify! x x0)
      (set-variable-unsensitivityify! x0 x)
      x0)))

(define (reverseify x)
 (or (variable-reverseify x)
     (let ((x0 (new-variable `(reverse ,(variable-name x)))))
      (when (and *order-limit* (>= (variable-order x) *order-limit*))
       (compile-time-error "Order limit exceeded"))
      (set-variable-reverseify! x x0)
      (set-variable-unreverseify! x0 x)
      x0)))

(define (unperturbationify x)
 (assert (variable-unperturbationify x))
 (variable-unperturbationify x))

(define (unforwardify x)
 (assert (variable-unforwardify x))
 (variable-unforwardify x))

(define (unsensitivityify? x)
 (or (variable-unsensitivityify x)
     (let loop ((x (variable-name x)))
      (and (pair? x)
	   (case (first x)
	    ;; This case needs to be this way because of the call to
	    ;; sensitivity-transform in reverse-transform-internal which is
	    ;; subsequently alpha-converted.
	    ((alpha) (loop (second x)))
	    ((anf) #f)
	    ((backpropagator) #f)
	    ((perturbation) #f)
	    ((forward) #f)
	    ((sensitivity) #t)
	    ((reverse) #f)
	    (else #f))))))

(define (unsensitivityify x)
 (or (variable-unsensitivityify x)
     (let ((x0 (new-variable
		(let loop ((x (variable-name x)))
		 (assert (pair? x))
		 (case (first x)
		  ;; This case needs to be this way because of the call to
		  ;; sensitivity-transform in reverse-transform-internal which
		  ;; is subsequently alpha-converted.
		  ((alpha) (loop (second x)))
		  ((anf) (internal-error))
		  ((backpropagator) (internal-error))
		  ((perturbation) (internal-error))
		  ((forward) (internal-error))
		  ((sensitivity) (second x))
		  ((reverse) (internal-error))
		  (else (internal-error)))))))
      (set-variable-unsensitivityify! x x0)
      x0)))

(define (unreverseify x)
 (assert (variable-unreverseify x))
 (variable-unreverseify x))

(define (sensitivity-access x)
 (new-variable-access-expression (sensitivityify x)))

(define (reverse-access x) (new-variable-access-expression (reverseify x)))

(define (perturbationify-access e)
 (new-variable-access-expression
  (perturbationify (variable-access-expression-variable e))))

(define (forwardify-access e)
 (new-variable-access-expression
  (forwardify (variable-access-expression-variable e))))

(define (sensitivityify-access e)
 (new-variable-access-expression
  (sensitivityify (variable-access-expression-variable e))))

(define (reverseify-access e)
 (new-variable-access-expression
  (reverseify (variable-access-expression-variable e))))

(define (variable-tags x)
 (let loop ((x (variable-name x)))
  (if (pair? x)
      (case (first x)
       ((alpha) (loop (second x)))
       ((anf) (empty-tags))
       ((backpropagator) (empty-tags))
       ((perturbation) (add-tag 'perturbation (loop (second x))))
       ((forward) (add-tag 'forward (loop (second x))))
       ((sensitivity) (add-tag 'sensitivity (loop (second x))))
       ((reverse) (add-tag 'reverse (loop (second x))))
       (else (internal-error)))
      (empty-tags))))

;;; Parameters

(define (parameter-tags p)
 (cond
  ;; Calling value-tags is OK because constant expression value should always
  ;; be concrete.
  ((constant-expression? p) (value-tags (constant-expression-value p)))
  ((variable-access-expression? p)
   (variable-tags (variable-access-expression-variable p)))
  ((lambda-expression? p) (lambda-expression-tags p))
  ((letrec-expression? p)
   (assert
    (and (variable-access-expression? (letrec-expression-body p))
	 (memp variable=?
	       (variable-access-expression-variable (letrec-expression-body p))
	       (letrec-expression-procedure-variables p))))
   ;; It is also possible to derive this from the tags of one of the procedure
   ;; variables.
   ;; The procedure-variables and lambda-expressions slots will be nonempty.
   (lambda-expression-tags (first (letrec-expression-lambda-expressions p))))
  ((cons-expression? p) (cons-expression-tags p))
  (else (internal-error))))

(define (lambda-expression-tags e)
 (parameter-tags (lambda-expression-parameter e)))

(define (perturbation-parameter? p) (tagged? 'perturbation (parameter-tags p)))

(define (forward-parameter? p) (tagged? 'forward (parameter-tags p)))

(define (sensitivity-parameter? p) (tagged? 'sensitivity (parameter-tags p)))

(define (reverse-parameter? p) (tagged? 'reverse (parameter-tags p)))

;;; Free variables

(define (union-variables xs1 xs2) (unionp variable=? xs1 xs2))

(define (free-variables e)
 (cond ((constant-expression? e) '())
       ((variable-access-expression? e)
	(list (variable-access-expression-variable e)))
       ((lambda-expression? e) (lambda-expression-free-variables e))
       ((application? e) (application-free-variables e))
       ((letrec-expression? e) (letrec-expression-free-variables e))
       ((cons-expression? e) (cons-expression-free-variables e))
       (else (internal-error))))

(define (recursive-closure-free-variables xs es)
 (sort-variables
  (set-differencep
   variable=? (map-reduce union-variables '() free-variables es) xs)))

(define (letrec-expression-variables e)
 (recursive-closure-free-variables (letrec-expression-procedure-variables e)
				   (letrec-expression-lambda-expressions e)))

(define (parameter-variables p)
 (cond ((constant-expression? p) '())
       ((variable-access-expression? p)
	(list (variable-access-expression-variable p)))
       ((lambda-expression? p) (free-variables p))
       ((letrec-expression? p)
	(assert (and (variable-access-expression? (letrec-expression-body p))
		     (memp variable=?
			   (variable-access-expression-variable
			    (letrec-expression-body p))
			   (letrec-expression-procedure-variables p))))
	(letrec-expression-variables p))
       ((cons-expression? p)
	(append (parameter-variables (cons-expression-car p))
		(parameter-variables (cons-expression-cdr p))))
       (else (internal-error))))

;;; Expression constructors

(define (new-constant-expression value)
 (let ((e0 (make-constant-expression '() '() value)))
  (set! *expressions* (cons e0 *expressions*))
  e0))

(define (new-variable-access-expression variable)
 (assert (variable? variable))
 (let ((e0 (make-variable-access-expression '() '() variable)))
  (set! *expressions* (cons e0 *expressions*))
  e0))

(define (new-lambda-expression p e)
 (assert (not (duplicatesp? variable=? (parameter-variables p))))
 (let ((e0 (make-lambda-expression
	    #f
	    #f
	    #f
	    #f
	    #f
	    #f
	    #f
	    #f
	    #f
	    '()
	    '()
	    '()
	    '()
	    (sort-variables
	     (set-differencep
	      variable=? (free-variables e) (parameter-variables p)))
	    p
	    e)))
  (set! *expressions* (cons e0 *expressions*))
  e0))

(define (new-application e1 e2)
 (let* ((xs (sort-variables
	     (union-variables (free-variables e1) (free-variables e2))))
	(e0 (make-application
	     '()
	     '()
	     #f
	     xs
	     e1
	     e2
	     (map (lambda (x) (positionp variable=? x xs))
		  (free-variables e1))
	     (map (lambda (x) (positionp variable=? x xs))
		  (free-variables e2)))))
  (set! *expressions* (cons e0 *expressions*))
  e0))

(define (new-letrec-expression xs es e)
 (assert (and (= (length xs) (length es)) (every variable? xs)))
 (if (null? xs)
     e
     (let* ((xs0 (sort-variables
		  (set-differencep
		   variable=?
		   (union-variables
		    (map-reduce union-variables '() free-variables es)
		    (free-variables e))
		   xs)))
	    (e0 (make-letrec-expression
		 '()
		 '()
		 #f
		 xs0
		 xs
		 es
		 e
		 (map (lambda (x) (positionp variable=? x xs0))
		      (recursive-closure-free-variables xs es)))))
      (set! *expressions* (cons e0 *expressions*))
      e0)))

(define (new-cons-expression tags e1 e2)
 (let* ((xs (sort-variables
	     (union-variables (free-variables e1) (free-variables e2))))
	(e0 (make-cons-expression
	     '()
	     '()
	     #f
	     xs
	     tags
	     e1
	     e2
	     (map (lambda (x) (positionp variable=? x xs))
		  (free-variables e1))
	     (map (lambda (x) (positionp variable=? x xs))
		  (free-variables e2)))))
  (set! *expressions* (cons e0 *expressions*))
  e0))

;;; Generic expression accessors and mutators

(define (expression-parents e)
 ((cond ((constant-expression? e) constant-expression-parents)
	((variable-access-expression? e) variable-access-expression-parents)
	((lambda-expression? e) lambda-expression-parents)
	((application? e) application-parents)
	((letrec-expression? e) letrec-expression-parents)
	((cons-expression? e) cons-expression-parents)
	(else (internal-error)))
  e))

(define (set-expression-parents! e es)
 ((cond
   ((constant-expression? e) set-constant-expression-parents!)
   ((variable-access-expression? e) set-variable-access-expression-parents!)
   ((lambda-expression? e) set-lambda-expression-parents!)
   ((application? e) set-application-parents!)
   ((letrec-expression? e) set-letrec-expression-parents!)
   ((cons-expression? e) set-cons-expression-parents!)
   (else (internal-error)))
  e
  es))

(define (expression-environment-bindings e)
 ((cond ((constant-expression? e) constant-expression-environment-bindings)
	((variable-access-expression? e)
	 variable-access-expression-environment-bindings)
	((lambda-expression? e) lambda-expression-environment-bindings)
	((application? e) application-environment-bindings)
	((letrec-expression? e) letrec-expression-environment-bindings)
	((cons-expression? e) cons-expression-environment-bindings)
	(else (internal-error)))
  e))

(define (set-expression-environment-bindings! e es)
 ((cond
   ((constant-expression? e) set-constant-expression-environment-bindings!)
   ((variable-access-expression? e)
    set-variable-access-expression-environment-bindings!)
   ((lambda-expression? e) set-lambda-expression-environment-bindings!)
   ((application? e) set-application-environment-bindings!)
   ((letrec-expression? e) set-letrec-expression-environment-bindings!)
   ((cons-expression? e) set-cons-expression-environment-bindings!)
   (else (internal-error)))
  e
  es))

(define (expression-enqueue? e)
 ((cond ((application? e) application-enqueue?)
	((letrec-expression? e) letrec-expression-enqueue?)
	((cons-expression? e) cons-expression-enqueue?)
	(else (internal-error)))
  e))

(define (set-expression-enqueue?! e es)
 ((cond ((application? e) set-application-enqueue?!)
	((letrec-expression? e) set-letrec-expression-enqueue?!)
	((cons-expression? e) set-cons-expression-enqueue?!)
	(else (internal-error)))
  e
  es))

;;; Derived expression constructors

(define (new-let* ps es e)
 (if (null? ps)
     e
     (new-application
      (new-lambda-expression (first ps) (new-let* (rest ps) (rest es) e))
      (first es))))

(define (create-let* bs e)
 (new-let* (map parameter-binding-parameter bs)
	   (map parameter-binding-expression bs)
	   e))

(define (reference-graph xs es e)
 ;; needs work: Should have structure instead of list.
 (list
  (map (lambda (x e) (list x (intersectionp variable=? xs (free-variables e))))
       xs
       es)
  (intersectionp variable=? xs (free-variables e))))

(define (transitive-closure arms)
 ;; needs work: Should have structure instead of list.
 (let loop ((arms arms))
  (let ((new-arms
	 (map (lambda (arm)
	       (list (first arm)
		     (union-variables
		      (second arm)
		      (map-reduce
		       union-variables
		       '()
		       (lambda (target) (second (assp variable=? target arms)))
		       (second arm)))))
	      arms)))
   (if (every (lambda (arm new-arm)
	       (set-equalq? (second arm) (second new-arm)))
	      arms
	      new-arms)
       arms
       (loop new-arms)))))

(define (strongly-connected-components arms transitive-arms)
 ;; needs work: Should have structure instead of list.
 (equivalence-classesp
  (lambda (x1 x2)
   (and (memp variable=? x1 (second (assp variable=? x2 transitive-arms)))
	(memp variable=? x2 (second (assp variable=? x1 transitive-arms)))))
  (map first arms)))

(define (lightweight-letrec-conversion xs es e)
 ;; needs work: Should have structure instead of list.
 (let* ((reference-graph (reference-graph xs es e))
	(arms (first reference-graph))
	(xs1 (second reference-graph))
	(transitive-arms (transitive-closure arms)))
  (map
   (lambda (this)
    (list
     this
     (or (not (null? (rest this)))
	 (not (not (memp variable=?
			 (first this)
			 (second (assp variable=? (first this) arms))))))))
   (topological-sort
    ;; component2 calls component1
    (lambda (component1 component2)
     (some (lambda (x2)
	    (some (lambda (x1)
		   (memp variable=?
			 x1
			 (second (assp variable=? x2 transitive-arms))))
		  component1))
	   component2))
    (remove-if-not
     (lambda (component)
      (some (lambda (x1)
	     (some (lambda (x2)
		    (or (variable=? x2 x1)
			(memp variable=?
			      x2
			      (second (assp variable=? x1 transitive-arms)))))
		   component))
	    xs1))
     (strongly-connected-components arms transitive-arms))))))

(define (create-letrec-expression xs es e)
 (let loop ((clusters (lightweight-letrec-conversion xs es e)))
  (if (null? clusters)
      e
      (let ((cluster (first clusters)))
       (if (second cluster)
	   (new-letrec-expression
	    (first cluster)
	    (map (lambda (x) (list-ref es (positionp variable=? x xs)))
		 (first cluster))
	    (loop (rest clusters)))
	   (let ((x (first (first cluster))))
	    (new-let* (list (new-variable-access-expression x))
		      (list (list-ref es (positionp variable=? x xs)))
		      (loop (rest clusters)))))))))

(define (create-cons-expression e1 e2)
 (new-cons-expression (empty-tags) e1 e2))

;;; LET*

(define (contains-letrec? e)
 (or (letrec-expression? e)
     (and (application? e)
	  (or (contains-letrec? (application-callee e))
	      (contains-letrec? (application-argument e))))
     (and (cons-expression? e)
	  (or (contains-letrec? (cons-expression-car e))
	      (contains-letrec? (cons-expression-cdr e))))))

(define (let*? e)
 ;; This is a stronger check than:
 ;;  2. No letrec nested in a let* expression or body can reference a variable
 ;;     bound by that let*.
 (and
  (application? e)
  (lambda-expression? (application-callee e))
  (and (not (contains-letrec? (lambda-expression-body (application-callee e))))
       (not (contains-letrec? (application-argument e))))))

(define (let*-parameters e)
 (if (let*? e)
     (cons (lambda-expression-parameter (application-callee e))
	   (let*-parameters (lambda-expression-body (application-callee e))))
     '()))

(define (let*-expressions e)
 (if (let*? e)
     (cons (application-argument e)
	   (let*-expressions (lambda-expression-body (application-callee e))))
     '()))

(define (let*-body e)
 (if (let*? e) (let*-body (lambda-expression-body (application-callee e))) e))

;;; Expression Equivalence

(define (expression-eqv? e1 e2)
 ;; needs work: We need to look for all implicit eq? comparisons.
 (eq? e1 e2))

(define (dereferenced-expression-eqv? e1 e2)
 ;; needs work: We need to look for all implicit eq? comparisons.
 (if (and (lambda-expression? e1) (lambda-expression? e2))
     (cond ((lambda-expression-alpha-conversion-inverse e1)
	    (dereferenced-expression-eqv?
	     (lambda-expression-alpha-conversion-inverse e1) e2))
	   ((lambda-expression-alpha-conversion-inverse e2)
	    (dereferenced-expression-eqv?
	     e1 (lambda-expression-alpha-conversion-inverse e2)))
	   ((and (lambda-expression-perturbation-transform-inverse e1)
		 (lambda-expression-perturbation-transform-inverse e2))
	    (dereferenced-expression-eqv?
	     (lambda-expression-perturbation-transform-inverse e1)
	     (lambda-expression-perturbation-transform-inverse e2)))
	   ((and (lambda-expression-forward-transform-inverse e1)
		 (lambda-expression-forward-transform-inverse e2))
	    (dereferenced-expression-eqv?
	     (lambda-expression-forward-transform-inverse e1)
	     (lambda-expression-forward-transform-inverse e2)))
	   ((and (lambda-expression-sensitivity-transform-inverse e1)
		 (lambda-expression-sensitivity-transform-inverse e2))
	    (dereferenced-expression-eqv?
	     (lambda-expression-sensitivity-transform-inverse e1)
	     (lambda-expression-sensitivity-transform-inverse e2)))
	   ((and (lambda-expression-reverse-transform-inverse e1)
		 (lambda-expression-reverse-transform-inverse e2))
	    (dereferenced-expression-eqv?
	     (lambda-expression-reverse-transform-inverse e1)
	     (lambda-expression-reverse-transform-inverse e2)))
	   (else (eq? e1 e2)))
     (eq? e1 e2)))

;;; Values

;;; Empty Lists

(define (vlad-empty-list) '())

;;; This used to (assert (not (union? u)))
(define (vlad-empty-list? u) (null? u))

(define (tagged-empty-list? tags u)
 (and
  (not (union? u))
  (or (and (empty-tags? tags) (vlad-empty-list? u))
      (and (tagged? 'perturbation tags)
	   (perturbation-tagged-value? u)
	   (tagged-empty-list? (remove-tag 'perturbation tags)
			       (perturbation-tagged-value-primal u)))
      (and (tagged? 'forward tags)
	   (bundle? u)
	   (tagged-empty-list? (remove-tag 'forward tags) (bundle-primal u))
	   (tagged-empty-list?
	    (add-tag 'perturbation (remove-tag 'forward tags))
	    (bundle-tangent u)))
      (and (tagged? 'sensitivity tags)
	   (sensitivity-tagged-value? u)
	   (tagged-empty-list? (remove-tag 'sensitivity tags)
			       (sensitivity-tagged-value-primal u)))
      (and (tagged? 'reverse tags)
	   (reverse-tagged-value? u)
	   (tagged-empty-list? (remove-tag 'reverse tags)
			       (reverse-tagged-value-primal u))))))

;;; Booleans

(define (vlad-true) #t)

(define (vlad-false) #f)

;;; This used to (assert (not (union? u)))
(define (vlad-true? u) (eq? u #t))

;;; This used to (assert (not (union? u)))
(define (vlad-false? u) (eq? u #f))

(define (vlad-boolean? u) (or (vlad-true? u) (vlad-false? u)))

;;; Reals

;;; This can't be real since there would be an ambiguity between an abstract
;;; real and the primitive real when externalizing.
(define (abstract-real) 'abstract-real)

;;; This used to (assert (not (union? u)))
(define (abstract-real? u)
 (eq? u 'abstract-real))

;;; This used to (assert (not (union? u)))
(define (vlad-real? u)
 (or (real? u) (abstract-real? u)))

;;; Closures

(define (allocate-nonrecursive-closure vs e)
 (make-nonrecursive-closure vs
			    e
			    #f
			    #f
			    #f
			    #f
			    #f
			    #f
			    #f
			    #f
			    #f
			    #f
			    #f
			    #f
			    #f
			    #f
			    'unfilled
			    #t
			    #t
			    #t
			    #t
			    #t
			    #t
			    #t
			    #t
			    #t))

(define (create-nonrecursive-closure vs e)
 (assert
  (and (= (length vs) (length (free-variables e)))
       ;; We used to enforce the constraint that the tags of the parameter of e
       ;; (as an indication of the tags of the resulting closure) were a prefix
       ;; of the tags of each v in vs. But this does not hold of the let*
       ;; bindings taken as lambda expressions for results paired with
       ;; backpropagator variables. The backpropagator variables are free in
       ;; the nested let* context context of the forward phase reverse
       ;; transformed procedure but the backpropagators are not reverse values.
       (or (eq? *mode* 'abstract)
	   (eq? *mode* 'symbolic)
	   (every
	    (lambda (x v) (prefix-tags? (variable-tags x) (value-tags v)))
	    (free-variables e)
	    vs))))
 (if (or
      (some empty-abstract-value? vs)
      (and (eq? *mode* 'abstract)
	   (some
	    (lambda (x v)
	     (every-value-tags
	      (lambda (tags) (not (prefix-tags? (variable-tags x) tags))) v))
	    (free-variables e)
	    vs)))
     (empty-abstract-value)
     (allocate-nonrecursive-closure vs e)))

(define (new-nonrecursive-closure vs e)
 (assert
  (and (= (length vs) (length (free-variables e)))
       ;; We used to enforce the constraint that the tags of the parameter of e
       ;; (as an indication of the tags of the resulting closure) were a prefix
       ;; of the tags of each v in vs. But this does not hold of the let*
       ;; bindings taken as lambda expressions for results paired with
       ;; backpropagator variables. The backpropagator variables are free in
       ;; the nested let* context context of the forward phase reverse
       ;; transformed procedure but the backpropagators are not reverse values.
       (or (eq? *mode* 'abstract)
	   (eq? *mode* 'symbolic)
	   (every
	    (lambda (x v) (prefix-tags? (variable-tags x) (value-tags v)))
	    (free-variables e)
	    vs))))
 (if (or
      (some empty-abstract-value? vs)
      (and (eq? *mode* 'abstract)
	   (some
	    (lambda (x v)
	     (every-value-tags
	      (lambda (tags) (not (prefix-tags? (variable-tags x) tags))) v))
	    (free-variables e)
	    vs)))
     (empty-abstract-value)
     (if *interned?*
	 ;; We only index on e. We could build an index tree on the vs. By
	 ;; indexing on e, there is an implicit expession-eqv?.
	 (or (find-if (lambda (v0)
		       ;; We don't need to check the lengths because if the
		       ;; expressions are expression-eqv?, they will have the
		       ;; same free variables.
		       (abstract-environment=?
			vs (get-nonrecursive-closure-values v0)))
		      (lambda-expression-nonrecursive-closures e))
	     (let ((v0 (allocate-nonrecursive-closure vs e)))
	      (assert (not *frozen?*))
	      (set-lambda-expression-nonrecursive-closures!
	       e (cons v0 (lambda-expression-nonrecursive-closures e)))
	      (set-nonrecursive-closure-canonize-cache! v0 v0)
	      (set-nonrecursive-closure-intern-cache! v0 v0)
	      v0))
	 (allocate-nonrecursive-closure vs e))))

(define (fill-nonrecursive-closure-values! u vs)
 ;; We can't do the full checks of new-nonrecursive-closure at this point
 ;; because there may be residual unfilled slots so the checks are delayed
 ;; until canonize-abstract-value.
 (assert
  (and (= (length vs)
	  (length (free-variables (nonrecursive-closure-lambda-expression u))))
       (eq? (nonrecursive-closure-values u) 'unfilled)))
 (set-nonrecursive-closure-values! u vs))

(define (get-nonrecursive-closure-values v)
 (assert (not (eq? (nonrecursive-closure-values v) 'unfilled)))
 (nonrecursive-closure-values v))

(define (allocate-recursive-closure vs xs es i)
 (make-recursive-closure vs
			 xs
			 es
			 i
			 #f
			 #f
			 #f
			 #f
			 #f
			 #f
			 #f
			 #f
			 #f
			 #f
			 #f
			 #f
			 #f
			 #f
			 'unfilled
			 #t
			 #t
			 #t
			 #t
			 #t
			 #t
			 #t
			 #t
			 #t))

(define (create-recursive-closure vs xs es i)
 (assert
  (and
   (= (length vs)
      (length (recursive-closure-free-variables
	       (vector->list xs) (vector->list es))))
   ;; See the note in new-nonrecursive-closure. While that hasn't happened in
   ;; practise, and I don't know whether it can, I removed it in principle.
   (or (eq? *mode* 'abstract)
       (eq? *mode* 'symbolic)
       (every
	(lambda (x v) (prefix-tags? (variable-tags x) (value-tags v)))
	(recursive-closure-free-variables (vector->list xs) (vector->list es))
	vs))))
 (if (or
      (some empty-abstract-value? vs)
      (and (eq? *mode* 'abstract)
	   (some
	    (lambda (x v)
	     (every-value-tags
	      (lambda (tags) (not (prefix-tags? (variable-tags x) tags))) v))
	    (recursive-closure-free-variables
	     (vector->list xs) (vector->list es))
	    vs)))
     (empty-abstract-value)
     (allocate-recursive-closure vs xs es i)))

(define (new-recursive-closure vs xs es i)
 (assert
  (and
   (= (length vs)
      (length (recursive-closure-free-variables
	       (vector->list xs) (vector->list es))))
   ;; See the note in new-nonrecursive-closure. While that hasn't happened in
   ;; practise, and I don't know whether it can, I removed it in principle.
   (or (eq? *mode* 'abstract)
       (eq? *mode* 'symbolic)
       (every
	(lambda (x v) (prefix-tags? (variable-tags x) (value-tags v)))
	(recursive-closure-free-variables (vector->list xs) (vector->list es))
	vs))))
 (if (or
      (some empty-abstract-value? vs)
      (and (eq? *mode* 'abstract)
	   (some
	    (lambda (x v)
	     (every-value-tags
	      (lambda (tags) (not (prefix-tags? (variable-tags x) tags))) v))
	    (recursive-closure-free-variables
	     (vector->list xs) (vector->list es))
	    vs)))
     (empty-abstract-value)
     (if *interned?*
	 ;; We only index on the first e. Since an e uniquely determines the
	 ;; letrec, we don't need to index by or even check the other es or the
	 ;; xs. We could index on i and build an index tree on the vs. By
	 ;; indexing on the first e, there is an implicit expession-eqv?.
	 (or (find-if
	      (lambda (v0)
	       (and
		(= i (recursive-closure-index v0))
		;; We don't need to check the lengths because if the letrec
		;; expressions are expression-eqv?, they will have the same
		;; free variables.
		(abstract-environment=? vs (get-recursive-closure-values v0))))
	      (lambda-expression-recursive-closures (vector-ref es 0)))
	     (let ((v0 (allocate-recursive-closure vs xs es i)))
	      (assert (not *frozen?*))
	      (set-lambda-expression-recursive-closures!
	       (vector-ref es 0)
	       (cons
		v0 (lambda-expression-recursive-closures (vector-ref es 0))))
	      (set-recursive-closure-canonize-cache! v0 v0)
	      (set-recursive-closure-intern-cache! v0 v0)
	      v0))
	 (allocate-recursive-closure vs xs es i))))

(define (fill-recursive-closure-values! u vs)
 ;; We can't do the full checks of new-recursive-closure at this point
 ;; because there may be residual unfilled slots so the checks are delayed
 ;; until canonize-abstract-value.
 (assert (and (= (length vs) (length (recursive-closure-variables u)))
	      (eq? (recursive-closure-values u) 'unfilled)))
 (set-recursive-closure-values! u vs))

(define (get-recursive-closure-values v)
 (assert (not (eq? (recursive-closure-values v) 'unfilled)))
 (recursive-closure-values v))

(define (nonrecursive-closure-match? u1 u2)
 (assert (and (not (union? u1)) (not (union? u2))))
 ;; The first condition is an optimization.
 (and (= (length (get-nonrecursive-closure-values u1))
	 (length (get-nonrecursive-closure-values u2)))
      (expression-eqv? (nonrecursive-closure-lambda-expression u1)
		       (nonrecursive-closure-lambda-expression u2))))

(define (dereferenced-nonrecursive-closure-match? u1 u2)
 (assert (and (not (union? u1)) (not (union? u2))))
 ;; The first condition is an optimization.
 (and (= (length (get-nonrecursive-closure-values u1))
	 (length (get-nonrecursive-closure-values u2)))
      (dereferenced-expression-eqv?
       (nonrecursive-closure-lambda-expression u1)
       (nonrecursive-closure-lambda-expression u2))))

(define (recursive-closure-match? u1 u2)
 (assert (and (not (union? u1)) (not (union? u2))))
 (and (= (recursive-closure-index u1) (recursive-closure-index u2))
      (= (vector-length (recursive-closure-procedure-variables u1))
	 (vector-length (recursive-closure-procedure-variables u2)))
      (= (vector-length (recursive-closure-lambda-expressions u1))
	 (vector-length (recursive-closure-lambda-expressions u2)))
      ;; This is an optimization.
      (= (length (get-recursive-closure-values u1))
	 (length (get-recursive-closure-values u2)))
      (every-vector expression-eqv?
		    (recursive-closure-lambda-expressions u1)
		    (recursive-closure-lambda-expressions u2))))

(define (dereferenced-recursive-closure-match? u1 u2)
 (assert (and (not (union? u1)) (not (union? u2))))
 (and (= (recursive-closure-index u1) (recursive-closure-index u2))
      (= (vector-length (recursive-closure-procedure-variables u1))
	 (vector-length (recursive-closure-procedure-variables u2)))
      (= (vector-length (recursive-closure-lambda-expressions u1))
	 (vector-length (recursive-closure-lambda-expressions u2)))
      ;; This is an optimization.
      (= (length (get-recursive-closure-values u1))
	 (length (get-recursive-closure-values u2)))
      (every-vector dereferenced-expression-eqv?
		    (recursive-closure-lambda-expressions u1)
		    (recursive-closure-lambda-expressions u2))))

;;; This used to (assert (not (union? u)))
(define (closure? u) (or (nonrecursive-closure? u) (recursive-closure? u)))

(define (closure-match? u1 u2)
 (assert (and (closure? u1) (closure? u2)))
 (or (and (nonrecursive-closure? u1)
	  (nonrecursive-closure? u2)
	  (nonrecursive-closure-match? u1 u2))
     (and (recursive-closure? u1)
	  (recursive-closure? u2)
	  (recursive-closure-match? u1 u2))))

(define (nonrecursive-closure-variables u)
 (assert (not (union? u)))
 (free-variables (nonrecursive-closure-lambda-expression u)))

(define (recursive-closure-variables u)
 (assert (not (union? u)))
 (recursive-closure-free-variables
  (vector->list (recursive-closure-procedure-variables u))
  (vector->list (recursive-closure-lambda-expressions u))))

(define (closure-variables u)
 (cond ((nonrecursive-closure? u) (nonrecursive-closure-variables u))
       ((recursive-closure? u) (recursive-closure-variables u))
       (else (internal-error))))

(define (nonrecursive-closure-parameter u)
 (assert (not (union? u)))
 (lambda-expression-parameter (nonrecursive-closure-lambda-expression u)))

(define (recursive-closure-parameter u)
 (assert (not (union? u)))
 (lambda-expression-parameter
  (vector-ref (recursive-closure-lambda-expressions u)
	      (recursive-closure-index u))))

(define (closure-parameter u)
 (cond ((nonrecursive-closure? u) (nonrecursive-closure-parameter u))
       ((recursive-closure? u) (recursive-closure-parameter u))
       (else (internal-error))))

(define (nonrecursive-closure-tags u)
 (assert (not (union? u)))
 (parameter-tags (nonrecursive-closure-parameter u)))

(define (recursive-closure-tags u)
 (assert (not (union? u)))
 (parameter-tags (recursive-closure-parameter u)))

(define (closure-body u)
 (lambda-expression-body
  (cond ((nonrecursive-closure? u) (nonrecursive-closure-lambda-expression u))
	((recursive-closure? u)
	 (vector-ref (recursive-closure-lambda-expressions u)
		     (recursive-closure-index u)))
	(else (internal-error)))))

(define (vlad-procedure? u)
 (assert (not (union? u)))
 (or (primitive-procedure? u) (closure? u)))

;;; Perturbation Tagged Values

(define (allocate-perturbation-tagged-value v)
 (make-perturbation-tagged-value v
				 #f
				 #f
				 #f
				 #f
				 #f
				 #f
				 #f
				 #f
				 #f
				 'unfilled
				 #t
				 #t
				 #t
				 #t
				 #t
				 #t
				 #t
				 #t
				 #t))

(define (create-perturbation-tagged-value v)
 (if (empty-abstract-value? v) v (allocate-perturbation-tagged-value v)))

(define (new-perturbation-tagged-value v)
 (if (empty-abstract-value? v)
     v
     (if *interned?*
	 (or (find-if
	      (lambda (v0)
	       (abstract-value=? v (get-perturbation-tagged-value-primal v0)))
	      *perturbation-tagged-values*)
	     (let ((v0 (allocate-perturbation-tagged-value v)))
	      (assert (not *frozen?*))
	      (set! *perturbation-tagged-values*
		    (cons v0 *perturbation-tagged-values*))
	      (set-perturbation-tagged-value-canonize-cache! v0 v0)
	      (set-perturbation-tagged-value-intern-cache! v0 v0)
	      v0))
	 (allocate-perturbation-tagged-value v))))

(define (fill-perturbation-tagged-value-primal! u v)
 ;; We can't do the full checks of new-perturbation-tagged-value at this point
 ;; because there may be residual unfilled slots so the checks are delayed
 ;; until canonize-abstract-value.
 (assert (eq? (perturbation-tagged-value-primal u) 'unfilled))
 (set-perturbation-tagged-value-primal! u v))

(define (get-perturbation-tagged-value-primal v)
 (assert (not (eq? (perturbation-tagged-value-primal v) 'unfilled)))
 (perturbation-tagged-value-primal v))

;;; Bundles

(define (some-bundlable? v v-perturbation)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  0
  (let loop ((v v)
	     (v-perturbation v-perturbation)
	     (cs '())
	     (k (lambda (r? cs) r?)))
   (let ((found?
	  (find-if
	   (lambda (c)
	    (and (eq? (car (car c)) v) (eq? (cdr (car c)) v-perturbation)))
	   cs)))
    (if found?
	(k (cdr found?) cs)
	;; needs work: What is the circular value?
	(let* ((c (cons (cons v v-perturbation) #f))
	       (cs (cons c cs))
	       (k (lambda (r? cs)
		   (set-cdr! c r?)
		   (k r? cs))))
	 (cond
	  ((union? v)
	   (some-cps (lambda (u cs k) (loop u v-perturbation cs k))
		     (union-members v)
		     cs
		     k))
	  ((union? v-perturbation)
	   (some-cps
	    (lambda (u-perturbation cs k) (loop v u-perturbation cs k))
	    (union-members v-perturbation)
	    cs
	    k))
	  ((or (and (vlad-empty-list? v)
		    (perturbation-tagged-value? v-perturbation)
		    (some vlad-empty-list?
			  (union-members
			   (get-perturbation-tagged-value-primal
			    v-perturbation))))
	       (and (vlad-true? v)
		    (perturbation-tagged-value? v-perturbation)
		    (some vlad-true?
			  (union-members
			   (get-perturbation-tagged-value-primal
			    v-perturbation))))
	       (and (vlad-false? v)
		    (perturbation-tagged-value? v-perturbation)
		    (some vlad-false?
			  (union-members
			   (get-perturbation-tagged-value-primal
			    v-perturbation))))
	       (and (vlad-real? v)
		    (perturbation-tagged-value? v-perturbation)
		    (some vlad-real?
			  (union-members
			   (get-perturbation-tagged-value-primal
			    v-perturbation))))
	       (and (primitive-procedure? v)
		    (perturbation-tagged-value? v-perturbation)
		    (some (lambda (u) (and (primitive-procedure? u) (eq? v u)))
			  (union-members
			   (get-perturbation-tagged-value-primal
			    v-perturbation)))))
	   (k #t cs))
	  ((and (perturbation-tagged-value? v)
		(perturbation-tagged-value? v-perturbation))
	   (some-cps
	    (lambda (u-perturbation cs k)
	     (if (perturbation-tagged-value? u-perturbation)
		 (loop (get-perturbation-tagged-value-primal v)
		       (create-perturbation-tagged-value
			(get-perturbation-tagged-value-primal u-perturbation))
		       cs
		       k)
		 (k #f cs)))
	    (union-members
	     (get-perturbation-tagged-value-primal v-perturbation))
	    cs
	    k))
	  ((and (bundle? v) (perturbation-tagged-value? v-perturbation))
	   (some-cps
	    (lambda (u-perturbation cs k)
	     (if (bundle? u-perturbation)
		 (loop (get-bundle-primal v)
		       (create-perturbation-tagged-value
			(get-bundle-primal u-perturbation))
		       cs
		       (lambda (r? cs)
			(if r?
			    (loop (get-bundle-tangent v)
				  (create-perturbation-tagged-value
				   (get-bundle-tangent u-perturbation))
				  cs
				  k)
			    (k #f cs))))
		 (k #f cs)))
	    (union-members
	     (get-perturbation-tagged-value-primal v-perturbation))
	    cs
	    k))
	  ((and (sensitivity-tagged-value? v)
		(perturbation-tagged-value? v-perturbation))
	   (some-cps
	    (lambda (u-perturbation cs k)
	     (if (sensitivity-tagged-value? u-perturbation)
		 (loop (get-sensitivity-tagged-value-primal v)
		       (create-perturbation-tagged-value
			(get-sensitivity-tagged-value-primal u-perturbation))
		       cs
		       k)
		 (k #f cs)))
	    (union-members
	     (get-perturbation-tagged-value-primal v-perturbation))
	    cs
	    k))
	  ((and (reverse-tagged-value? v)
		(perturbation-tagged-value? v-perturbation))
	   (some-cps
	    (lambda (u-perturbation cs k)
	     (if (reverse-tagged-value? u-perturbation)
		 (loop (get-reverse-tagged-value-primal v)
		       (create-perturbation-tagged-value
			(get-reverse-tagged-value-primal u-perturbation))
		       cs
		       k)
		 (k #f cs)))
	    (union-members
	     (get-perturbation-tagged-value-primal v-perturbation))
	    cs
	    k))
	  (else (k #f cs)))))))))

(define (every-bundlable? v v-perturbation)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  1
  (let loop ((v v)
	     (v-perturbation v-perturbation)
	     (cs '())
	     (k (lambda (r? cs) r?)))
   (let ((found?
	  (find-if
	   (lambda (c)
	    (and (eq? (car (car c)) v) (eq? (cdr (car c)) v-perturbation)))
	   cs)))
    (if found?
	(k (cdr found?) cs)
	;; needs work: What is the circular value?
	(let* ((c (cons (cons v v-perturbation) #t))
	       (cs (cons c cs))
	       (k (lambda (r? cs)
		   (set-cdr! c r?)
		   (k r? cs))))
	 (cond
	  ((union? v)
	   (every-cps (lambda (u cs k) (loop u v-perturbation cs k))
		      (union-members v)
		      cs
		      k))
	  ((union? v-perturbation)
	   (every-cps
	    (lambda (u-perturbation cs k) (loop v u-perturbation cs k))
	    (union-members v-perturbation)
	    cs
	    k))
	  ((or
	    (and
	     (vlad-empty-list? v)
	     (perturbation-tagged-value? v-perturbation)
	     (every vlad-empty-list?
		    (union-members
		     (get-perturbation-tagged-value-primal v-perturbation))))
	    (and
	     (vlad-true? v)
	     (perturbation-tagged-value? v-perturbation)
	     (every vlad-true?
		    (union-members
		     (get-perturbation-tagged-value-primal v-perturbation))))
	    (and
	     (vlad-false? v)
	     (perturbation-tagged-value? v-perturbation)
	     (every vlad-false?
		    (union-members
		     (get-perturbation-tagged-value-primal v-perturbation))))
	    (and
	     (vlad-real? v)
	     (perturbation-tagged-value? v-perturbation)
	     (every vlad-real?
		    (union-members
		     (get-perturbation-tagged-value-primal v-perturbation))))
	    (and
	     (primitive-procedure? v)
	     (perturbation-tagged-value? v-perturbation)
	     (every (lambda (u) (and (primitive-procedure? u) (eq? v u)))
		    (union-members
		     (get-perturbation-tagged-value-primal v-perturbation)))))
	   (k #t cs))
	  ((and (perturbation-tagged-value? v)
		(perturbation-tagged-value? v-perturbation))
	   (every-cps
	    (lambda (u-perturbation cs k)
	     (if (perturbation-tagged-value? u-perturbation)
		 (loop (get-perturbation-tagged-value-primal v)
		       (create-perturbation-tagged-value
			(get-perturbation-tagged-value-primal u-perturbation))
		       cs
		       k)
		 (k #f cs)))
	    (union-members
	     (get-perturbation-tagged-value-primal v-perturbation))
	    cs
	    k))
	  ((and (bundle? v) (perturbation-tagged-value? v-perturbation))
	   (every-cps
	    (lambda (u-perturbation cs k)
	     (if (bundle? u-perturbation)
		 (loop (get-bundle-primal v)
		       (create-perturbation-tagged-value
			(get-bundle-primal u-perturbation))
		       cs
		       (lambda (r? cs)
			(if r?
			    (loop (get-bundle-tangent v)
				  (create-perturbation-tagged-value
				   (get-bundle-tangent u-perturbation))
				  cs
				  k)
			    (k #f cs))))
		 (k #f cs)))
	    (union-members
	     (get-perturbation-tagged-value-primal v-perturbation))
	    cs
	    k))
	  ((and (sensitivity-tagged-value? v)
		(perturbation-tagged-value? v-perturbation))
	   (every-cps
	    (lambda (u-perturbation cs k)
	     (if (sensitivity-tagged-value? u-perturbation)
		 (loop (get-sensitivity-tagged-value-primal v)
		       (create-perturbation-tagged-value
			(get-sensitivity-tagged-value-primal u-perturbation))
		       cs
		       k)
		 (k #f cs)))
	    (union-members
	     (get-perturbation-tagged-value-primal v-perturbation))
	    cs
	    k))
	  ((and (reverse-tagged-value? v)
		(perturbation-tagged-value? v-perturbation))
	   (every-cps
	    (lambda (u-perturbation cs k)
	     (if (reverse-tagged-value? u-perturbation)
		 (loop (get-reverse-tagged-value-primal v)
		       (create-perturbation-tagged-value
			(get-reverse-tagged-value-primal u-perturbation))
		       cs
		       k)
		 (k #f cs)))
	    (union-members
	     (get-perturbation-tagged-value-primal v-perturbation))
	    cs
	    k))
	  (else (k #f cs)))))))))

(define (allocate-bundle v v-perturbation)
 (make-bundle v
	      v-perturbation
	      #f
	      #f
	      #f
	      #f
	      #f
	      #f
	      #f
	      #f
	      #f
	      'unfilled
	      #t
	      #t
	      #t
	      #t
	      #t
	      #t
	      #t
	      #t
	      #t))

(define (create-bundle v v-perturbation)
 (assert (or (eq? *mode* 'abstract)
	     (eq? *mode* 'symbolic)
	     (some-bundlable? v v-perturbation)))
 (if (or (empty-abstract-value? v)
	 (empty-abstract-value? v-perturbation)
	 (and (eq? *mode* 'abstract) (not (some-bundlable? v v-perturbation))))
     (empty-abstract-value)
     (allocate-bundle v v-perturbation)))

(define (new-bundle v v-perturbation)
 (assert (or (eq? *mode* 'abstract)
	     (eq? *mode* 'symbolic)
	     (some-bundlable? v v-perturbation)))
 (if (or (empty-abstract-value? v)
	 (empty-abstract-value? v-perturbation)
	 (and (eq? *mode* 'abstract) (not (some-bundlable? v v-perturbation))))
     (empty-abstract-value)
     (if *interned?*
	 (or (find-if
	      (lambda (v0)
	       (and (abstract-value=? v (get-bundle-primal v0))
		    (abstract-value=? v-perturbation (get-bundle-tangent v0))))
	      *bundles*)
	     (let ((v0 (allocate-bundle v v-perturbation)))
	      (assert (not *frozen?*))
	      (set! *bundles* (cons v0 *bundles*))
	      (set-bundle-canonize-cache! v0 v0)
	      (set-bundle-intern-cache! v0 v0)
	      v0))
	 (allocate-bundle v v-perturbation))))

(define (fill-bundle! u v v-perturbation)
 ;; We can't do the full checks of new-bundle at this point because there may
 ;; be residual unfilled slots so the checks are delayed until
 ;; canonize-abstract-value.
 (assert (and (eq? (bundle-primal u) 'unfilled)
	      (eq? (bundle-tangent u) 'unfilled)))
 (set-bundle-primal! u v)
 (set-bundle-tangent! u v-perturbation))

(define (get-bundle-primal v)
 (assert (not (eq? (bundle-primal v) 'unfilled)))
 (bundle-primal v))

(define (get-bundle-tangent v)
 (assert (not (eq? (bundle-tangent v) 'unfilled)))
 (bundle-tangent v))

;;; Sensitivity Tagged Values

(define (allocate-sensitivity-tagged-value v)
 (make-sensitivity-tagged-value v
				#f
				#f
				#f
				#f
				#f
				#f
				#f
				#f
				#f
				'unfilled
				#t
				#t
				#t
				#t
				#t
				#t
				#t
				#t
				#t))

(define (create-sensitivity-tagged-value v)
 (if (empty-abstract-value? v) v (allocate-sensitivity-tagged-value v)))

(define (new-sensitivity-tagged-value v)
 (if (empty-abstract-value? v)
     v
     (if *interned?*
	 (or (find-if
	      (lambda (v0)
	       (abstract-value=? v (get-sensitivity-tagged-value-primal v0)))
	      *sensitivity-tagged-values*)
	     (let ((v0 (allocate-sensitivity-tagged-value v)))
	      (assert (not *frozen?*))
	      (set! *sensitivity-tagged-values*
		    (cons v0 *sensitivity-tagged-values*))
	      (set-sensitivity-tagged-value-canonize-cache! v0 v0)
	      (set-sensitivity-tagged-value-intern-cache! v0 v0)
	      v0))
	 (allocate-sensitivity-tagged-value v))))

(define (fill-sensitivity-tagged-value-primal! u v)
 ;; We can't do the full checks of new-sensitivity-tagged-value at this point
 ;; because there may be residual unfilled slots so the checks are delayed
 ;; until canonize-abstract-value.
 (assert (eq? (sensitivity-tagged-value-primal u) 'unfilled))
 (set-sensitivity-tagged-value-primal! u v))

(define (get-sensitivity-tagged-value-primal v)
 (assert (not (eq? (sensitivity-tagged-value-primal v) 'unfilled)))
 (sensitivity-tagged-value-primal v))

;;; Reverse Tagged Values

(define (allocate-reverse-tagged-value v)
 (make-reverse-tagged-value v
			    #f
			    #f
			    #f
			    #f
			    #f
			    #f
			    #f
			    #f
			    #f
			    'unfilled
			    #t
			    #t
			    #t
			    #t
			    #t
			    #t
			    #t
			    #t
			    #t))

(define (create-reverse-tagged-value v)
 (if (empty-abstract-value? v) v (allocate-reverse-tagged-value v)))

(define (new-reverse-tagged-value v)
 (if (empty-abstract-value? v)
     v
     (if *interned?*
	 (or (find-if
	      (lambda (v0)
	       (abstract-value=? v (get-reverse-tagged-value-primal v0)))
	      *reverse-tagged-values*)
	     (let ((v0 (allocate-reverse-tagged-value v)))
	      (assert (not *frozen?*))
	      (set! *reverse-tagged-values* (cons v0 *reverse-tagged-values*))
	      (set-reverse-tagged-value-canonize-cache! v0 v0)
	      (set-reverse-tagged-value-intern-cache! v0 v0)
	      v0))
	 (allocate-reverse-tagged-value v))))

(define (fill-reverse-tagged-value-primal! u v)
 ;; We can't do the full checks of new-reverse-tagged-value at this point
 ;; because there may be residual unfilled slots so the checks are delayed
 ;; until canonize-abstract-value.
 (assert (eq? (reverse-tagged-value-primal u) 'unfilled))
 (set-reverse-tagged-value-primal! u v))

(define (get-reverse-tagged-value-primal v)
 (assert (not (eq? (reverse-tagged-value-primal v) 'unfilled)))
 (reverse-tagged-value-primal v))

;;; Pairs

(define (allocate-tagged-pair tags v1 v2)
 (make-tagged-pair tags
		   v1
		   v2
		   #f
		   #f
		   #f
		   #f
		   #f
		   #f
		   #f
		   #f
		   #f
		   #f
		   #f
		   #f
		   #f
		   #f
		   #f
		   #f
		   'unfilled
		   #t
		   #t
		   #t
		   #t
		   #t
		   #t
		   #t
		   #t
		   #t
		   #t
		   #t))

(define (create-tagged-pair tags v1 v2)
 (assert (or (eq? *mode* 'abstract)
	     (eq? *mode* 'symbolic)
	     (and (prefix-tags? tags (value-tags v1))
		  (prefix-tags? tags (value-tags v2)))))
 (if (or
      (empty-abstract-value? v1)
      (empty-abstract-value? v2)
      (and
       (eq? *mode* 'abstract)
       (or (every-value-tags (lambda (tags) (not (prefix-tags? tags tags))) v1)
	   (every-value-tags (lambda (tags) (not (prefix-tags? tags tags)))
			     v2))))
     (empty-abstract-value)
     (allocate-tagged-pair tags v1 v2)))

(define (new-tagged-pair tags v1 v2)
 (assert (or (eq? *mode* 'abstract)
	     (eq? *mode* 'symbolic)
	     (and (prefix-tags? tags (value-tags v1))
		  (prefix-tags? tags (value-tags v2)))))
 (if (or
      (empty-abstract-value? v1)
      (empty-abstract-value? v2)
      (and
       (eq? *mode* 'abstract)
       (or (every-value-tags (lambda (tags) (not (prefix-tags? tags tags))) v1)
	   (every-value-tags (lambda (tags) (not (prefix-tags? tags tags)))
			     v2))))
     (empty-abstract-value)
     (if *interned?*
	 (or (find-if (lambda (v0)
		       (and (abstract-value=? v1 (get-tagged-pair-car v0))
			    (abstract-value=? v2 (get-tagged-pair-cdr v0))
			    (equal-tags? tags (tagged-pair-tags v0))))
		      *tagged-pairs*)
	     (let ((v0 (allocate-tagged-pair tags v1 v2)))
	      (assert (not *frozen?*))
	      (set! *tagged-pairs* (cons v0 *tagged-pairs*))
	      (set-tagged-pair-canonize-cache! v0 v0)
	      (set-tagged-pair-intern-cache! v0 v0)
	      v0))
	 (allocate-tagged-pair tags v1 v2))))

(define (fill-tagged-pair! u v1 v2)
 ;; We can't do the full checks of new-tagged-pair at this point because there
 ;; may be residual unfilled slots so the checks are delayed until
 ;; canonize-abstract-value.
 (assert (and (eq? (tagged-pair-car u) 'unfilled)
	      (eq? (tagged-pair-cdr u) 'unfilled)))
 (set-tagged-pair-car! u v1)
 (set-tagged-pair-cdr! u v2))

(define (get-tagged-pair-car v)
 (assert (not (eq? (tagged-pair-car v) 'unfilled)))
 (tagged-pair-car v))

(define (get-tagged-pair-cdr v)
 (assert (not (eq? (tagged-pair-cdr v) 'unfilled)))
 (tagged-pair-cdr v))

;;; This used to (assert (not (union? u)))
(define (vlad-pair? u)
 (and (tagged-pair? u) (empty-tags? (tagged-pair-tags u))))

(define (vlad-car u)
 (assert (vlad-pair? u))
 (get-tagged-pair-car u))

(define (vlad-cdr u)
 (assert (vlad-pair? u))
 (get-tagged-pair-cdr u))

(define (vlad-cons v1 v2) (new-tagged-pair (empty-tags) v1 v2))

;;; Unions

(define (empty-abstract-value) *empty-abstract-value*)

(define (empty-abstract-value? v) (null? (union-members v)))

(define (abstract-boolean) *abstract-boolean*)

(define (union-members v)
 (if (union? v)
     (map-reduce append '() union-members (get-union-values v))
     (list v)))

(define (allocate-union vs)
 (make-union vs
	     #f
	     #f
	     #f
	     #f
	     #f
	     #f
	     #f
	     #f
	     #f
	     #f
	     #f
	     #f
	     #f
	     #f
	     #f
	     #f
	     'unfilled
	     #t
	     #t
	     #t
	     #t
	     #t
	     #t
	     #t
	     #t
	     #t
	     #t
	     #t
	     'unfilled))

(define (create-union vs) (allocate-union vs))

(define (new-union vs)
 (if *interned?*
     (or (find-if
	  (lambda (v0) (set-equalp? abstract-value=? vs (get-union-values v0)))
	  *unions*)
	 (let ((v0 (allocate-union vs)))
	  (assert (not *frozen?*))
	  (set! *unions* (cons v0 *unions*))
	  (set-union-canonize-cache! v0 v0)
	  (set-union-intern-cache! v0 v0)
	  v0))
     (allocate-union vs)))

(define (fill-union-values! v vs)
 (assert (and (not (memq v vs)) (eq? (union-values v) 'unfilled)))
 (set-union-values! v vs))

(define (get-union-values v)
 (assert (not (eq? (union-values v) 'unfilled)))
 (union-values v))

(define (unionize vs) (reduce abstract-value-union vs (empty-abstract-value)))

(define (map-union f v) (unionize (map f (union-members v))))

(define (cross-union f v1 v2)
 (unionize (cross-product f (union-members v1) (union-members v2))))

;;; Units

(define (new-name-unit abstract-value code)
 (let ((u (make-name-unit abstract-value code)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-call-unit abstract-value code . xs)
 (let ((u (make-call-unit abstract-value code xs)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-panic-unit x)
 (let ((u (make-panic-unit x)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-+-unit x y)
 (let ((u (make-+-unit x y)))
  (set! *units* (append *units* (list u)))
  u))

(define (new---unit x y)
 (let ((u (make---unit x y)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-*-unit x y)
 (let ((u (make-*-unit x y)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-/-unit x y)
 (let ((u (make-/-unit x y)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-sqrt-unit x)
 (let ((u (make-sqrt-unit x)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-exp-unit x)
 (let ((u (make-exp-unit x)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-log-unit x)
 (let ((u (make-log-unit x)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-sin-unit x)
 (let ((u (make-sin-unit x)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-cos-unit x)
 (let ((u (make-cos-unit x)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-atan-unit x y)
 (let ((u (make-atan-unit x y)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-=-unit x y)
 (let ((u (make-=-unit x y)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-<-unit x y)
 (let ((u (make-<-unit x y)))
  (set! *units* (append *units* (list u)))
  u))

(define (new->-unit x y)
 (let ((u (make->-unit x y)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-<=-unit x y)
 (let ((u (make-<=-unit x y)))
  (set! *units* (append *units* (list u)))
  u))

(define (new->=-unit x y)
 (let ((u (make->=-unit x y)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-zero?-unit x)
 (let ((u (make-zero?-unit x)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-positive?-unit x)
 (let ((u (make-positive?-unit x)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-negative?-unit x)
 (let ((u (make-negative?-unit x)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-read-real-unit x)
 (let ((u (make-read-real-unit x)))
  (set! *units* (append *units* (list u)))
  u))

(define (new-write-real-unit x)
 (let ((u (make-write-real-unit x)))
  (set! *units* (append *units* (list u)))
  u))

;;; Generic

(define (perturbation-value? u)
 (assert (not (union? u)))
 (or (and (nonrecursive-closure? u)
	  (perturbation-parameter? (nonrecursive-closure-parameter u)))
     (and (recursive-closure? u)
	  (perturbation-parameter? (recursive-closure-parameter u)))
     (perturbation-tagged-value? u)
     (and (tagged-pair? u) (tagged? 'perturbation (tagged-pair-tags u)))))

(define (forward-value? u)
 (assert (not (union? u)))
 (or (and (nonrecursive-closure? u)
	  (forward-parameter? (nonrecursive-closure-parameter u)))
     (and (recursive-closure? u)
	  (forward-parameter? (recursive-closure-parameter u)))
     (bundle? u)
     (and (tagged-pair? u) (tagged? 'forward (tagged-pair-tags u)))))

(define (sensitivity-value? u)
 ;; Backpropagators will be considered as sensitivity values. But you can't
 ;; unsensitize them. You need to invoke backpropagators so we can't prohibit
 ;; invocation of sensitivity-tagged procedures. Perhaps we could/should
 ;; prohibit invocation of perturbation-tagged procedures.
 (assert (not (union? u)))
 (or (and (nonrecursive-closure? u)
	  (sensitivity-parameter? (nonrecursive-closure-parameter u)))
     (and (recursive-closure? u)
	  (sensitivity-parameter? (recursive-closure-parameter u)))
     (sensitivity-tagged-value? u)
     (and (tagged-pair? u) (tagged? 'sensitivity (tagged-pair-tags u)))))

(define (reverse-value? u)
 (assert (not (union? u)))
 (or (and (nonrecursive-closure? u)
	  (reverse-parameter? (nonrecursive-closure-parameter u)))
     (and (recursive-closure? u)
	  (reverse-parameter? (recursive-closure-parameter u)))
     (reverse-tagged-value? u)
     (and (tagged-pair? u) (tagged? 'reverse (tagged-pair-tags u)))))

(define (scalar-value? u)
 (assert (not (union? u)))
 (or (vlad-boolean? u)
     (vlad-empty-list? u)
     (vlad-real? u)
     (primitive-procedure? u)))

(define (aggregate-value-values u)
 (cond
  ((nonrecursive-closure? u) (get-nonrecursive-closure-values u))
  ((recursive-closure? u) (get-recursive-closure-values u))
  ((perturbation-tagged-value? u)
   (list (get-perturbation-tagged-value-primal u)))
  ((bundle? u) (list (get-bundle-primal u) (get-bundle-tangent u)))
  ((sensitivity-tagged-value? u)
   (list (get-sensitivity-tagged-value-primal u)))
  ((reverse-tagged-value? u) (list (get-reverse-tagged-value-primal u)))
  ((tagged-pair? u) (list (get-tagged-pair-car u) (get-tagged-pair-cdr u)))
  (else (internal-error))))

(define (create-aggregate-value-with-new-values u vs)
 (cond
  ((nonrecursive-closure? u)
   (create-nonrecursive-closure vs (nonrecursive-closure-lambda-expression u)))
  ((recursive-closure? u)
   (create-recursive-closure vs
			     (recursive-closure-procedure-variables u)
			     (recursive-closure-lambda-expressions u)
			     (recursive-closure-index u)))
  ((perturbation-tagged-value? u)
   (assert (= (length vs) 1))
   (create-perturbation-tagged-value (first vs)))
  ((bundle? u)
   (assert (= (length vs) 2))
   (create-bundle (first vs) (second vs)))
  ((sensitivity-tagged-value? u)
   (assert (= (length vs) 1))
   (create-sensitivity-tagged-value (first vs)))
  ((reverse-tagged-value? u)
   (assert (= (length vs) 1))
   (create-reverse-tagged-value (first vs)))
  ((tagged-pair? u)
   (assert (= (length vs) 2))
   (create-tagged-pair (tagged-pair-tags u) (first vs) (second vs)))
  (else (internal-error))))

(define (value-tags u)
 (assert (not (union? u)))
 (cond ((scalar-value? u) '())
       ((nonrecursive-closure? u) (nonrecursive-closure-tags u))
       ((recursive-closure? u) (recursive-closure-tags u))
       ((perturbation-tagged-value? u)
	(add-tag 'perturbation
		 (value-tags (get-perturbation-tagged-value-primal u))))
       ;; needs work: When we add unions one might be able to get a more
       ;;             precise answer by traversing both the primal and the
       ;;             tangent.
       ((bundle? u) (add-tag 'forward (value-tags (get-bundle-primal u))))
       ((sensitivity-tagged-value? u)
	(add-tag 'sensitivity
		 (value-tags (get-sensitivity-tagged-value-primal u))))
       ((reverse-tagged-value? u)
	(add-tag 'reverse (value-tags (get-reverse-tagged-value-primal u))))
       ((tagged-pair? u) (tagged-pair-tags u))
       (else (internal-error))))

(define (some-value-tags p v)
 (let loop ((tags '()) (v v) (vs '()))
  (cond
   ;; needs work: I'm not sure that this is sound.
   ((memq v vs) #t)
   ((union? v) (some (lambda (u) (loop tags u (cons v vs))) (union-members v)))
   ((scalar-value? v) (p (reverse tags)))
   ((nonrecursive-closure? v)
    (p (append (reverse tags) (nonrecursive-closure-tags v))))
   ((recursive-closure? v)
    (p (append (reverse tags) (recursive-closure-tags v))))
   ((perturbation-tagged-value? v)
    (loop (cons 'perturbation tags)
	  (get-perturbation-tagged-value-primal v) vs))
   ((bundle? v) (loop (cons 'forward tags) (get-bundle-primal v) vs))
   ((sensitivity-tagged-value? v)
    (loop (cons 'sensitivity tags) (get-sensitivity-tagged-value-primal v) vs))
   ((reverse-tagged-value? v)
    (loop (cons 'reverse tags) (get-reverse-tagged-value-primal v) vs))
   ((tagged-pair? v) (p (append (reverse tags) (tagged-pair-tags v))))
   (else (internal-error)))))

(define (every-value-tags p v)
 (let loop ((tags '()) (v v) (vs '()))
  (cond
   ;; needs work: I'm not sure that this is sound.
   ((memq v vs) #f)
   ((union? v)
    (every (lambda (u) (loop tags u (cons v vs))) (union-members v)))
   ((scalar-value? v) (p (reverse tags)))
   ((nonrecursive-closure? v)
    (p (append (reverse tags) (nonrecursive-closure-tags v))))
   ((recursive-closure? v)
    (p (append (reverse tags) (recursive-closure-tags v))))
   ((perturbation-tagged-value? v)
    (loop (cons 'perturbation tags)
	  (get-perturbation-tagged-value-primal v) vs))
   ((bundle? v) (loop (cons 'forward tags) (get-bundle-primal v) vs))
   ((sensitivity-tagged-value? v)
    (loop (cons 'sensitivity tags) (get-sensitivity-tagged-value-primal v) vs))
   ((reverse-tagged-value? v)
    (loop (cons 'reverse tags) (get-reverse-tagged-value-primal v) vs))
   ((tagged-pair? v) (p (append (reverse tags) (tagged-pair-tags v))))
   (else (internal-error)))))

;;; Abstract Value Subset, Equivalence, Nondisjointness, Union, Canonization,
;;; and Internment

(define (abstract-value-subset? v1 v2)
 ;; I used to think that abstract value subset and equality is undecidable (by
 ;; reduction from context-free-grammar equivalence and that it is
 ;; semidecidable since a lone element in the extension of the left argument
 ;; that is not in the extension of the right argument witnesses nonsubset and
 ;; the extension of an abstract value is recursively enumerable.) But now I
 ;; realize that we are asking about the trees generated by a grammar, not the
 ;; strings, i.e. strong equivalence, not weak equivalence. And I don't know
 ;; whether this is decidable. We conservatively approximate these. A #t result
 ;; is precise. The lone cause of imprecision is illustrated by the following
 ;; example. Let v1={box({0,1})} and v2={box({0}),box({1})}. v1 is a subset of
 ;; v2. Yet the procedure checks whether for every u1 in v1 there is some u2 in
 ;; v2 such that u1 is a subset of v2. This does not hold in this example
 ;; because there is no single u2 which box({0,1}) is a subset of. One can get
 ;; more precision by multiplying out v1. In this case, multiplying out v1 to
 ;; {box({0}),box({1})} whould allow every u1 to have a single u2 for which u1
 ;; is a subset of u2. Thus in this case, multiplying out would yield a precise
 ;; result. In principle, one only need multiply out v1. But if v1 has
 ;; recursion, there is no bound on the amount of multiplying out that may be
 ;; needed.
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  2
  (let loop ((v1 v1) (v2 v2) (cs '()) (k (lambda (r? cs) r?)))
   (let ((found?
	  (find-if
	   (lambda (c) (and (eq? (car (car c)) v1) (eq? (cdr (car c)) v2)))
	   cs)))
    (if found?
	(k (cdr found?) cs)
	;; needs work: What is the circular value?
	(let* ((c (cons (cons v1 v2) #t))
	       (cs (cons c cs))
	       (k (lambda (r? cs)
		   (set-cdr! c r?)
		   (k r? cs))))
	 (cond
	  ;; This is an optimization.
	  ((eq? v1 v2) (k #t cs))
	  ((union? v1)
	   (every-cps
	    (lambda (u1 cs k) (loop u1 v2 cs k)) (union-members v1) cs k))
	  ((union? v2)
	   (some-cps
	    (lambda (u2 cs k) (loop v1 u2 cs k)) (union-members v2) cs k))
	  ((or (and (vlad-empty-list? v1) (vlad-empty-list? v2))
	       (and (vlad-true? v1) (vlad-true? v2))
	       (and (vlad-false? v1) (vlad-false? v2))
	       (and (vlad-real? v1)
		    (vlad-real? v2)
		    ;; This was = but then it equates exact values with inexact
		    ;; values and this breaks -imprecise-inexacts.
		    (or (and (real? v1) (real? v2) (equal? v1 v2))
			(and (real? v1) (abstract-real? v2))
			(and (abstract-real? v1) (abstract-real? v2))))
	       (and (primitive-procedure? v1)
		    (primitive-procedure? v2)
		    (eq? v1 v2)))
	   (k #t cs))
	  ((and (nonrecursive-closure? v1)
		(nonrecursive-closure? v2)
		(nonrecursive-closure-match? v1 v2))
	   ;; See the note in abstract-environment=?.
	   (every2-cps loop
		       (get-nonrecursive-closure-values v1)
		       (get-nonrecursive-closure-values v2)
		       cs
		       k))
	  ((and (recursive-closure? v1)
		(recursive-closure? v2)
		(recursive-closure-match? v1 v2))
	   ;; See the note in abstract-environment=?.
	   (every2-cps loop
		       (get-recursive-closure-values v1)
		       (get-recursive-closure-values v2)
		       cs
		       k))
	  ((and (perturbation-tagged-value? v1)
		(perturbation-tagged-value? v2))
	   (loop (get-perturbation-tagged-value-primal v1)
		 (get-perturbation-tagged-value-primal v2)
		 cs
		 k))
	  ((and (bundle? v1) (bundle? v2))
	   (loop (get-bundle-primal v1)
		 (get-bundle-primal v2)
		 cs
		 (lambda (r? cs)
		  (if r?
		      (loop (get-bundle-tangent v1)
			    (get-bundle-tangent v2)
			    cs
			    k)
		      (k #f cs)))))
	  ((and (sensitivity-tagged-value? v1) (sensitivity-tagged-value? v2))
	   (loop (get-sensitivity-tagged-value-primal v1)
		 (get-sensitivity-tagged-value-primal v2)
		 cs
		 k))
	  ((and (reverse-tagged-value? v1) (reverse-tagged-value? v2))
	   (loop (get-reverse-tagged-value-primal v1)
		 (get-reverse-tagged-value-primal v2)
		 cs
		 k))
	  ((and (tagged-pair? v1)
		(tagged-pair? v2)
		(equal-tags? (tagged-pair-tags v1) (tagged-pair-tags v2)))
	   (loop (get-tagged-pair-car v1)
		 (get-tagged-pair-car v2)
		 cs
		 (lambda (r? cs)
		  (if r?
		      (loop (get-tagged-pair-cdr v1)
			    (get-tagged-pair-cdr v2)
			    cs
			    k)
		      (k #f cs)))))
	  (else (k #f cs)))))))))

(define (deep-abstract-value=? v1 v2)
 (and (abstract-value-subset? v1 v2) (abstract-value-subset? v2 v1)))

(define (abstract-value=? v1 v2)
 (if *interned?*
     ;; This works because vlad-empty-list is (), vlad-true is #t, vlad-false
     ;; is #f, abstract-real is real, and all other non-concrete-real values
     ;; are structures. All of these are comparable with eq?.
     (or (eq? v1 v2) (and (real? v1) (real? v2) (equal? v1 v2)))
     (deep-abstract-value=? v1 v2)))

(define (filled-abstract-value-subset? v1 v2)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  3
  (let loop ((v1 v1) (v2 v2) (cs '()) (k (lambda (r? cs) r?)))
   (let ((found?
	  (find-if
	   (lambda (c) (and (eq? (car (car c)) v1) (eq? (cdr (car c)) v2)))
	   cs)))
    (if found?
	(k (cdr found?) cs)
	;; needs work: What is the circular value?
	(let* ((c (cons (cons v1 v2) #t))
	       (cs (cons c cs))
	       (k (lambda (r? cs)
		   (set-cdr! c r?)
		   (k r? cs))))
	 (cond
	  ;; This is an optimization.
	  ((eq? v1 v2) (k #t cs))
	  ((union? v1)
	   (if (eq? (union-values v1) 'unfilled)
	       (k #f cs)
	       (every-cps
		(lambda (u1 cs k) (loop u1 v2 cs k)) (union-members v1) cs k)))
	  ((union? v2)
	   (if (eq? (union-values v2) 'unfilled)
	       (k #f cs)
	       (some-cps
		(lambda (u2 cs k) (loop v1 u2 cs k)) (union-members v2) cs k)))
	  ((or (and (vlad-empty-list? v1) (vlad-empty-list? v2))
	       (and (vlad-true? v1) (vlad-true? v2))
	       (and (vlad-false? v1) (vlad-false? v2))
	       (and (vlad-real? v1)
		    (vlad-real? v2)
		    ;; This was = but then it equates exact values with inexact
		    ;; values and this breaks -imprecise-inexacts.
		    (or (and (real? v1) (real? v2) (equal? v1 v2))
			(and (real? v1) (abstract-real? v2))
			(and (abstract-real? v1) (abstract-real? v2))))
	       (and (primitive-procedure? v1)
		    (primitive-procedure? v2)
		    (eq? v1 v2)))
	   (k #t cs))
	  ((and (nonrecursive-closure? v1)
		(nonrecursive-closure? v2)
		(not (eq? (nonrecursive-closure-values v1) 'unfilled))
		(not (eq? (nonrecursive-closure-values v2) 'unfilled))
		(nonrecursive-closure-match? v1 v2))
	   ;; See the note in abstract-environment=?.
	   (every2-cps loop
		       (get-nonrecursive-closure-values v1)
		       (get-nonrecursive-closure-values v2)
		       cs
		       k))
	  ((and (recursive-closure? v1)
		(recursive-closure? v2)
		(not (eq? (recursive-closure-values v1) 'unfilled))
		(not (eq? (recursive-closure-values v2) 'unfilled))
		(recursive-closure-match? v1 v2))
	   ;; See the note in abstract-environment=?.
	   (every2-cps loop
		       (get-recursive-closure-values v1)
		       (get-recursive-closure-values v2)
		       cs
		       k))
	  ((and (perturbation-tagged-value? v1)
		(perturbation-tagged-value? v2)
		(not (eq? (perturbation-tagged-value-primal v1) 'unfilled))
		(not (eq? (perturbation-tagged-value-primal v2) 'unfilled)))
	   (loop (get-perturbation-tagged-value-primal v1)
		 (get-perturbation-tagged-value-primal v2)
		 cs
		 k))
	  ((and (bundle? v1)
		(bundle? v2)
		(not (eq? (bundle-primal v1) 'unfilled))
		(not (eq? (bundle-tangent v1) 'unfilled))
		(not (eq? (bundle-primal v2) 'unfilled))
		(not (eq? (bundle-tangent v2) 'unfilled)))
	   (loop (get-bundle-primal v1)
		 (get-bundle-primal v2)
		 cs
		 (lambda (r? cs)
		  (if r?
		      (loop (get-bundle-tangent v1)
			    (get-bundle-tangent v2)
			    cs
			    k)
		      (k #f cs)))))
	  ((and (sensitivity-tagged-value? v1)
		(sensitivity-tagged-value? v2)
		(not (eq? (sensitivity-tagged-value-primal v1) 'unfilled))
		(not (eq? (sensitivity-tagged-value-primal v2) 'unfilled)))
	   (loop (get-sensitivity-tagged-value-primal v1)
		 (get-sensitivity-tagged-value-primal v2)
		 cs
		 k))
	  ((and (reverse-tagged-value? v1)
		(reverse-tagged-value? v2)
		(not (eq? (reverse-tagged-value-primal v1) 'unfilled))
		(not (eq? (reverse-tagged-value-primal v2) 'unfilled)))
	   (loop (get-reverse-tagged-value-primal v1)
		 (get-reverse-tagged-value-primal v2)
		 cs
		 k))
	  ((and (tagged-pair? v1)
		(tagged-pair? v2)
		(not (eq? (tagged-pair-car v1) 'unfilled))
		(not (eq? (tagged-pair-cdr v1) 'unfilled))
		(not (eq? (tagged-pair-car v2) 'unfilled))
		(not (eq? (tagged-pair-cdr v2) 'unfilled))
		(equal-tags? (tagged-pair-tags v1) (tagged-pair-tags v2)))
	   (loop (get-tagged-pair-car v1)
		 (get-tagged-pair-car v2)
		 cs
		 (lambda (r? cs)
		  (if r?
		      (loop (get-tagged-pair-cdr v1)
			    (get-tagged-pair-cdr v2)
			    cs
			    k)
		      (k #f cs)))))
	  (else (k #f cs)))))))))

(define (filled-deep-abstract-value=? v1 v2)
 (and (filled-abstract-value-subset? v1 v2)
      (filled-abstract-value-subset? v2 v1)))

(define (abstract-value-nondisjoint? v1 v2)
 ;; I used to think that determining whether two abstract values are
 ;; nondisjoint is undecidable (by reduction from nonempty interesection of
 ;; two context-free grammars, which is semidecidable since a lone element in
 ;; the extension of both arguments witnesses nondisjointness and the
 ;; extension of an abstract value is enumerable.) But now I realize that we
 ;; are asking about the trees generated by a grammar, not the strings, i.e.
 ;; strong equivalence, not weak equivalence. And I believe that determining
 ;; whether the set of trees generated by two context-free grammars is
 ;; nondisjoint is decidable. And I believe that this algorithm is precise.
 ;; Only used in abstract-destructure and generate-destructure for generating
 ;; error messages.
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  4
  (let loop ((v1 v1) (v2 v2) (cs '()) (k (lambda (r? cs) r?)))
   (let ((found?
	  (find-if
	   (lambda (c) (and (eq? (car (car c)) v1) (eq? (cdr (car c)) v2)))
	   cs)))
    (if found?
	(k (cdr found?) cs)
	;; needs work: What is the circular value?
	(let* ((c (cons (cons v1 v2) #f))
	       (cs (cons c cs))
	       (k (lambda (r? cs)
		   (set-cdr! c r?)
		   (k r? cs))))
	 (cond
	  ;; This is an optimization.
	  ((and (eq? v1 v2) (not (empty-abstract-value? v1))) (k #t cs))
	  ((union? v1)
	   (some-cps
	    (lambda (u1 cs k) (loop u1 v2 cs k)) (union-members v1) cs k))
	  ((union? v2)
	   (let ((c (cons (cons v1 v2) #f)))
	    (some-cps
	     (lambda (u2 cs k) (loop v1 u2 cs k)) (union-members v2) cs k)))
	  ((or (and (vlad-empty-list? v1) (vlad-empty-list? v2))
	       (and (vlad-true? v1) (vlad-true? v2))
	       (and (vlad-false? v1) (vlad-false? v2))
	       (and (vlad-real? v1)
		    (vlad-real? v2)
		    (or (abstract-real? v1)
			(abstract-real? v2)
			;; This was = but then it equates exact values with
			;; inexact values and this breaks -imprecise-inexacts.
			(equal? v1 v2)))
	       (and (primitive-procedure? v1)
		    (primitive-procedure? v2)
		    (eq? v1 v2)))
	   (k #t cs))
	  ((and (nonrecursive-closure? v1)
		(nonrecursive-closure? v2)
		(nonrecursive-closure-match? v1 v2))
	   ;; See the note in abstract-environment=?.
	   (every2-cps loop
		       (get-nonrecursive-closure-values v1)
		       (get-nonrecursive-closure-values v2)
		       cs
		       k))
	  ((and (recursive-closure? v1)
		(recursive-closure? v2)
		(recursive-closure-match? v1 v2))
	   ;; See the note in abstract-environment=?.
	   (every2-cps loop
		       (get-recursive-closure-values v1)
		       (get-recursive-closure-values v2)
		       cs
		       k))
	  ((and (perturbation-tagged-value? v1)
		(perturbation-tagged-value? v2))
	   (loop (get-perturbation-tagged-value-primal v1)
		 (get-perturbation-tagged-value-primal v2)
		 cs
		 k))
	  ((and (bundle? v1) (bundle? v2))
	   (loop (get-bundle-primal v1)
		 (get-bundle-primal v2)
		 cs
		 (lambda (r? cs)
		  (if r?
		      (loop (get-bundle-tangent v1)
			    (get-bundle-tangent v2)
			    cs
			    k)
		      (k #f cs)))))
	  ((and (sensitivity-tagged-value? v1) (sensitivity-tagged-value? v2))
	   (loop (get-sensitivity-tagged-value-primal v1)
		 (get-sensitivity-tagged-value-primal v2)
		 cs
		 k))
	  ((and (reverse-tagged-value? v1) (reverse-tagged-value? v2))
	   (loop (get-reverse-tagged-value-primal v1)
		 (get-reverse-tagged-value-primal v2)
		 cs
		 k))
	  ((and (tagged-pair? v1)
		(tagged-pair? v2)
		(equal-tags? (tagged-pair-tags v1) (tagged-pair-tags v2)))
	   (loop (get-tagged-pair-car v1)
		 (get-tagged-pair-car v2)
		 cs
		 (lambda (r? cs)
		  (if r?
		      (loop (get-tagged-pair-cdr v1)
			    (get-tagged-pair-cdr v2)
			    cs
			    k)
		      (k #f cs)))))
	  (else (k #f cs)))))))))

(define (abstract-value-unionable? p? v1 v2)
 ;; When p? is true asks whether unionable without creating new top-level
 ;; union.
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  5
  (or (not *almost-union-free?*)
      (let loop ((p? p?) (v1 v1) (v2 v2) (cs '()) (k (lambda (r? cs) r?)))
       (let ((found?
	      (find-if
	       (lambda (c) (and (eq? (car (car c)) v1) (eq? (cdr (car c)) v2)))
	       cs)))
	(if found?
	    (k (cdr found?) cs)
	    ;; needs work: What is the circular value?
	    (let* ((c (cons (cons v1 v2) #t))
		   (cs (cons c cs))
		   (k (lambda (r? cs)
		       (set-cdr! c r?)
		       (k r? cs))))
	     (cond
	      ;; This is an optimization.
	      ((eq? v1 v2) (k #t cs))
	      ((or (union? v1) (union? v2))
	       (every-cps
		(lambda (u1 cs k)
		 (some-cps (lambda (u2 cs k) (loop #t u1 u2 cs k))
			   (union-members v2)
			   cs
			   k))
		(union-members v1)
		cs
		(lambda (r? cs)
		 (if r?
		     (k #t cs)
		     (every-cps
		      (lambda (u2 cs k)
		       (some-cps (lambda (u1 cs k) (loop #t u1 u2 cs k))
				 (union-members v1)
				 cs
				 k))
		      (union-members v2)
		      cs
		      k)))))
	      ((or (and (vlad-empty-list? v1) (vlad-empty-list? v2))
		   (and (vlad-true? v1) (vlad-true? v2))
		   (and (vlad-false? v1) (vlad-false? v2))
		   (and (vlad-boolean? v1) (vlad-boolean? v2) (not p?))
		   (and (vlad-real? v1) (vlad-real? v2))
		   (and (primitive-procedure? v1)
			(primitive-procedure? v2)
			(eq? v1 v2))
		   (and (backpropagator? v1) (backpropagator? v2) (not p?))
		   ;; needs work: I use union-members instead of
		   ;;             get-union-values below since v1 and v2 might
		   ;;             not be canonical because unionable? is
		   ;;             called in union-internal which is called on
		   ;;             values that might not be canonical and also
		   ;;             by widen-lists on arguments that might not
		   ;;             be canonical because widen-lists calls and
		   ;;             processes the result of union-internal. This
		   ;;             whole mess needs to be cleaned up.
		   (and (not p?)
			(tagged-pair? v2)
			(tagged-empty-list? (tagged-pair-tags v2) v1)
			(union? (tagged-pair-cdr v2))
			(= (length (union-members (tagged-pair-cdr v2))) 2)
			(some (lambda (u2)
			       (tagged-empty-list? (tagged-pair-tags v2) u2))
			      (union-members (tagged-pair-cdr v2)))
			(some (lambda (u2) (deep-abstract-value=? u2 v2))
			      (union-members (tagged-pair-cdr v2))))
		   (and (not p?)
			(tagged-pair? v1)
			(tagged-empty-list? (tagged-pair-tags v1) v2)
			(union? (tagged-pair-cdr v1))
			(= (length (union-members (tagged-pair-cdr v1))) 2)
			(some (lambda (u1)
			       (tagged-empty-list? (tagged-pair-tags v1) u1))
			      (union-members (tagged-pair-cdr v1)))
			(some (lambda (u1) (deep-abstract-value=? u1 v1))
			      (union-members (tagged-pair-cdr v1)))))
	       (k #t cs))
	      ((and (nonrecursive-closure? v1)
		    (nonrecursive-closure? v2)
		    (nonrecursive-closure-match? v1 v2))
	       ;; See the note in abstract-environment=?.
	       (every2-cps (lambda (u1 u2 cs k) (loop #f u1 u2 cs k))
			   (get-nonrecursive-closure-values v1)
			   (get-nonrecursive-closure-values v2)
			   cs
			   k))
	      ((and (recursive-closure? v1)
		    (recursive-closure? v2)
		    (recursive-closure-match? v1 v2))
	       ;; See the note in abstract-environment=?.
	       (every2-cps (lambda (u1 u2 cs k) (loop #f u1 u2 cs k))
			   (get-recursive-closure-values v1)
			   (get-recursive-closure-values v2)
			   cs
			   k))
	      ((and (perturbation-tagged-value? v1)
		    (perturbation-tagged-value? v2))
	       (loop #f
		     (get-perturbation-tagged-value-primal v1)
		     (get-perturbation-tagged-value-primal v2)
		     cs
		     k))
	      ((and (bundle? v1) (bundle? v2))
	       (loop #f
		     (get-bundle-primal v1)
		     (get-bundle-primal v2)
		     cs
		     (lambda (r? cs)
		      (if r?
			  (loop #f
				(get-bundle-tangent v1)
				(get-bundle-tangent v2)
				cs
				k)
			  (k #f cs)))))
	      ((and (sensitivity-tagged-value? v1)
		    (sensitivity-tagged-value? v2))
	       (loop #f
		     (get-sensitivity-tagged-value-primal v1)
		     (get-sensitivity-tagged-value-primal v2)
		     cs
		     k))
	      ((and (reverse-tagged-value? v1) (reverse-tagged-value? v2))
	       (loop #f
		     (get-reverse-tagged-value-primal v1)
		     (get-reverse-tagged-value-primal v2)
		     cs
		     k))
	      ((and (tagged-pair? v1)
		    (tagged-pair? v2)
		    (equal-tags? (tagged-pair-tags v1) (tagged-pair-tags v2)))
	       (loop #f
		     (get-tagged-pair-car v1)
		     (get-tagged-pair-car v2)
		     cs
		     (lambda (r? cs)
		      (if r?
			  (loop #f
				(get-tagged-pair-cdr v1)
				(get-tagged-pair-cdr v2)
				cs
				k)
			  (k #f cs)))))
	      (else (k #f cs))))))))))

(define (abstract-value-union-internal v1 v2)
 ;; This is written in CPS so as not to break structure sharing.
 ;; The output can be wider than the strict union since unions of transformed
 ;; booleans are transformed into transformed unions of booleans, widening in
 ;; the process (for bundles).
 (time-it-bucket
  6
  (if *almost-union-free?*
      (let loop ((v1 v1) (v2 v2) (cs '()) (k (lambda (v cs) v)))
       (let ((found?
	      (find-if (lambda (c)
			(and (eq? (car (car c)) v1) (eq? (cdr (car c)) v2)))
		       cs)))
	(cond
	 (found? (k (cdr found?) cs))
	 ;; needs work: These two cases were added because of
	 ;;             examples/generator-tests/bug{1,2,3,4}. This bug happens
	 ;;             because loop has the invariant that while the result
	 ;;             can be unfilled, the argumets must be filled. But
	 ;;             reduce-cps violates this invariant. I don't know how to
	 ;;             properly fix this. The next two cases hide the bug but
	 ;;             I'm not sure that they solve it.
	 ((abstract-value-subset? v1 v2) (k v2 cs))
	 ((abstract-value-subset? v2 v1) (k v1 cs))
	 ;; needs work: I added the restriction to unionable?-#t to preclude
	 ;;             the error check in fill-union-values that happens
	 ;;             when unioning (union #t #f) with #f. But I fear that
	 ;;             this now precludes unions of more than two elements,
	 ;;             inter alia, more than two backpropagators.
	 ((or (union? v1) (union? v2))
	  (cond
	   ((every (lambda (u1)
		    (some (lambda (u2) (abstract-value-unionable? #t u1 u2))
			  (union-members v2)))
		   (union-members v1))
	    (let ((u12s
		   (map (lambda (u1)
			 (cons u1
			       (find-if (lambda (u2)
					 (abstract-value-unionable? #t u1 u2))
					(union-members v2))))
			(union-members v1)))
		  (v (allocate-union 'unfilled)))
	     (map-cps
	      (lambda (u2 cs k)
	       (reduce-cps
		loop
		(map car
		     (remove-if-not (lambda (u12) (eq? (cdr u12) u2)) u12s))
		u2
		cs
		k))
	      (union-members v2)
	      (cons (cons (cons v1 v2) v) cs)
	      (lambda (us cs)
	       (fill-union-values! v us)
	       (k v cs)))))
	   ((every (lambda (u2)
		    (some (lambda (u1) (abstract-value-unionable? #t u1 u2))
			  (union-members v1)))
		   (union-members v2))
	    (let ((u21s
		   (map (lambda (u2)
			 (cons u2
			       (find-if (lambda (u1)
					 (abstract-value-unionable? #t u1 u2))
					(union-members v1))))
			(union-members v2)))
		  (v (allocate-union 'unfilled)))
	     (map-cps
	      (lambda (u1 cs k)
	       (reduce-cps
		loop
		(map car
		     (remove-if-not (lambda (u21) (eq? (cdr u21) u1)) u21s))
		u1
		cs
		k))
	      (union-members v1)
	      (cons (cons (cons v1 v2) v) cs)
	      (lambda (us cs)
	       (fill-union-values! v us)
	       (k v cs)))))
	   ;; needs work: Sometimes anerror occures in externalize because
	   ;;             v1 and/or v2 might not be canonical since
	   ;;             abstract-value-union-internal is called inside
	   ;;             canonize-abstractvalue.
	   (else (compile-time-error "Program is not almost union free: ~s ~s"
				     (externalize v1)
				     (externalize v2)))))
	 ((and (vlad-empty-list? v1) (vlad-empty-list? v2))
	  (let ((u v1)) (k u (cons (cons (cons v1 v2) u) cs))))
	 ((and (vlad-true? v1) (vlad-true? v2))
	  (let ((u v1)) (k u (cons (cons (cons v1 v2) u) cs))))
	 ((and (vlad-false? v1) (vlad-false? v2))
	  (let ((u v1)) (k u (cons (cons (cons v1 v2) u) cs))))
	 ((and (vlad-boolean? v1) (vlad-boolean? v2))
	  (let ((u (abstract-boolean))) (k u (cons (cons (cons v1 v2) u) cs))))
	 ((and (real? v1) (real? v2) (equal? v1 v2))
	  (let ((u v1)) (k u (cons (cons (cons v1 v2) u) cs))))
	 ((and (vlad-real? v1) (vlad-real? v2))
	  (let ((u (abstract-real))) (k u (cons (cons (cons v1 v2) u) cs))))
	 ((and (primitive-procedure? v1) (primitive-procedure? v2) (eq? v1 v2))
	  (let ((u v1)) (k u (cons (cons (cons v1 v2) u) cs))))
	 ;; See note in abstract-value-unionable?.
	 ((and (tagged-pair? v2)
	       (tagged-empty-list? (tagged-pair-tags v2) v1)
	       (union? (tagged-pair-cdr v2))
	       (= (length (union-members (tagged-pair-cdr v2))) 2)
	       (some (lambda (u2)
		      (tagged-empty-list? (tagged-pair-tags v2) u2))
		     (union-members (tagged-pair-cdr v2)))
	       (some (lambda (u2) (deep-abstract-value=? u2 v2))
		     (union-members (tagged-pair-cdr v2))))
	  (let ((u (tagged-pair-cdr v2)))
	   (k u (cons (cons (cons v1 v2) u) cs))))
	 ((and (tagged-pair? v1)
	       (tagged-empty-list? (tagged-pair-tags v1) v2)
	       (union? (tagged-pair-cdr v1))
	       (= (length (union-members (tagged-pair-cdr v1))) 2)
	       (some (lambda (u1)
		      (tagged-empty-list? (tagged-pair-tags v1) u1))
		     (union-members (tagged-pair-cdr v1)))
	       (some (lambda (u1) (deep-abstract-value=? u1 v1))
		     (union-members (tagged-pair-cdr v1))))
	  (let ((u (tagged-pair-cdr v1)))
	   (k u (cons (cons (cons v1 v2) u) cs))))
	 ((and (nonrecursive-closure? v1)
	       (nonrecursive-closure? v2)
	       (nonrecursive-closure-match? v1 v2)
	       (every (lambda (v1 v2) (abstract-value-unionable? #f v1 v2))
		      (get-nonrecursive-closure-values v1)
		      (get-nonrecursive-closure-values v2)))
	  ;; See the note in abstract-environment=?.
	  (let ((u (allocate-nonrecursive-closure
		    'unfilled (nonrecursive-closure-lambda-expression v1))))
	   (map2-cps loop
		     (get-nonrecursive-closure-values v1)
		     (get-nonrecursive-closure-values v2)
		     (cons (cons (cons v1 v2) u) cs)
		     (lambda (vs cs)
		      (fill-nonrecursive-closure-values! u vs)
		      (k u cs)))))
	 ((and (backpropagator? v1) (backpropagator? v2))
	  ;; I removed a check whether v1 and v2 where deep-abstract-value=?
	  ;; here since I believe that that check is subsumed by the above.
	  (let ((u (create-union (list v1 v2))))
	   (k u (cons (cons (cons v1 v2) u) cs))))
	 ((and (recursive-closure? v1)
	       (recursive-closure? v2)
	       (recursive-closure-match? v1 v2))
	  ;; See the note in abstract-environment=?.
	  (let ((u (allocate-recursive-closure
		    'unfilled
		    (recursive-closure-procedure-variables v1)
		    (recursive-closure-lambda-expressions v1)
		    (recursive-closure-index v1))))
	   (map2-cps loop
		     (get-recursive-closure-values v1)
		     (get-recursive-closure-values v2)
		     (cons (cons (cons v1 v2) u) cs)
		     (lambda (vs cs)
		      (fill-recursive-closure-values! u vs)
		      (k u cs)))))
	 ((and (perturbation-tagged-value? v1) (perturbation-tagged-value? v2))
	  (let ((u (allocate-perturbation-tagged-value 'unfilled)))
	   (loop (get-perturbation-tagged-value-primal v1)
		 (get-perturbation-tagged-value-primal v2)
		 (cons (cons (cons v1 v2) u) cs)
		 (lambda (v cs)
		  (fill-perturbation-tagged-value-primal! u v)
		  (k u cs)))))
	 ((and (bundle? v1) (bundle? v2))
	  (let ((u (allocate-bundle 'unfilled 'unfilled)))
	   (loop (get-bundle-primal v1)
		 (get-bundle-primal v2)
		 (cons (cons (cons v1 v2) u) cs)
		 (lambda (v-primal cs)
		  (loop (get-bundle-tangent v1)
			(get-bundle-tangent v2)
			cs
			(lambda (v-tangent cs)
			 (fill-bundle! u v-primal v-tangent)
			 (k u cs)))))))
	 ((and (sensitivity-tagged-value? v1) (sensitivity-tagged-value? v2))
	  (let ((u (allocate-sensitivity-tagged-value 'unfilled)))
	   (loop (get-sensitivity-tagged-value-primal v1)
		 (get-sensitivity-tagged-value-primal v2)
		 (cons (cons (cons v1 v2) u) cs)
		 (lambda (v cs)
		  (fill-sensitivity-tagged-value-primal! u v)
		  (k u cs)))))
	 ((and (reverse-tagged-value? v1) (reverse-tagged-value? v2))
	  (let ((u (allocate-reverse-tagged-value 'unfilled)))
	   (loop (get-reverse-tagged-value-primal v1)
		 (get-reverse-tagged-value-primal v2)
		 (cons (cons (cons v1 v2) u) cs)
		 (lambda (v cs)
		  (fill-reverse-tagged-value-primal! u v)
		  (k u cs)))))
	 ((and (tagged-pair? v1)
	       (tagged-pair? v2)
	       (equal-tags? (tagged-pair-tags v1) (tagged-pair-tags v2)))
	  (let ((u (allocate-tagged-pair
		    (tagged-pair-tags v1) 'unfilled 'unfilled)))
	   (loop (get-tagged-pair-car v1)
		 (get-tagged-pair-car v2)
		 (cons (cons (cons v1 v2) u) cs)
		 (lambda (v-car cs)
		  (loop (get-tagged-pair-cdr v1)
			(get-tagged-pair-cdr v2)
			cs
			(lambda (v-cdr cs)
			 (fill-tagged-pair! u v-car v-cdr)
			 (k u cs)))))))
	 ;; needs work: See note above.
	 (else (compile-time-error "Program is not almost union free: ~s ~s"
				   (externalize v1)
				   (externalize v2))))))
      (cond ((abstract-value-subset? v1 v2) v2)
	    ((abstract-value-subset? v2 v1) v1)
	    (else (create-union
		   (maximal-elements
		    abstract-value-subset?
		    (append (union-members v1) (union-members v2)))))))))

(define (abstract-value-union v1 v2)
 (canonize-and-maybe-intern-abstract-value
  (abstract-value-union-internal v1 v2)))

(define (debugging-canonize-abstract-value v)
 ;; needs work: This is a rewrite which is an attempt to clean up the union
 ;;             case. It is not finished, does not work, and does not handle
 ;;             transforming unions of aggregates into aggregates of unions.
 ;; This is written in CPS so as not to break structure sharing.
 ;; The whole purpose of this procedure is to:
 ;; - propagate empty abstract values (empty unions) upward so that there are
 ;;   never any nested empty abstract values,
 ;; - to merge unions of unions so that there are never any unions immediately
 ;;   nested in another union,
 ;; - to remove singleton unions, and
 ;; - to propagate unions of transformed booleans into transformed unions of
 ;;   booleans. For bundles, this widens in the process.
 ;; If assq is replaced with assp deep-abstract-value=? then this also:
 ;; - discovers structure to share.
 ;; It is necessary to use assp deep-abstract-value=? or else an error occurs
 ;; during nr-sqrt-RR where equivalent but non-eq recursive abstract values are
 ;; nested and then path-of-greatest-depth finds a path with equivalent but
 ;; non-eq values which when merged yield a value that again has that path
 ;; causing an infinite loop.
 (time-it-bucket
  7
  (let loop ((v v) (cs '()) (k (lambda (v-prime cs) v-prime)))
   (let ((found? (assp deep-abstract-value=? v cs)))
    (cond
     (found? (k (cdr found?) cs))
     ((union? v)
      (cond
       ((union-canonize-cache v) (k (union-canonize-cache v) cs))
       ((deep-empty-abstract-value? v)
	(let ((u-prime (empty-abstract-value)))
	 (k u-prime (cons (cons v u-prime) cs))))
       (else
	;; This is the whole reason we require that abstract values be copied.
	;; This performs the optimization that unionize performs but
	;; fill-union-values! is unable to because of unfilled slots.
	(let ((v-prime (allocate-union 'unfilled)))
	 ;; We cannot (set-union-canonize-cache! v v-prime) or
	 ;; (set-union-canonize-cache! v-prime v-prime) here because of the
	 ;; singleton case (the first branch of the cond) below.
	 (map-cps loop
		  (union-members v)
		  ;; We cannot (cons (cons v v-prime) cs) here for the same
		  ;; reason as above.
		  cs
		  (lambda (us-prime cs)
		   (assert (and (not (some union? us-prime))))
		   ;; needs work
		   ;; Still need to transform unions of transformed booleans
		   ;; into transformed unions of booleans when union-freeness
		   ;; must be preserved. Only want to do this in this case
		   ;; since it widens.
		   (cond ((and (not (null? us-prime)) (null? (rest us-prime)))
			  (set-union-canonize-cache! v (first us-prime))
			  (k (first us-prime) cs))
			 (else (set-union-canonize-cache! v v-prime)
			       (set-union-canonize-cache! v-prime v-prime)
			       (fill-union-values! v-prime us-prime)
			       (k v-prime cs)))))))))
     ((vlad-empty-list? v)
      (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
     ((vlad-true? v)
      (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
     ((vlad-false? v)
      (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
     ((vlad-real? v)
      (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
     ((primitive-procedure? v)
      (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
     ((nonrecursive-closure? v)
      (cond
       ((nonrecursive-closure-canonize-cache v)
	(k (nonrecursive-closure-canonize-cache v) cs))
       ((deep-empty-abstract-value? v)
	(let ((u-prime (empty-abstract-value)))
	 (k u-prime (cons (cons v u-prime) cs))))
       (else
	;; See the note in abstract-environment=?.
	(let ((u-prime (allocate-nonrecursive-closure
			'unfilled (nonrecursive-closure-lambda-expression v))))
	 (assert
	  (and (= (length (get-nonrecursive-closure-values v))
		  (length (free-variables
			   (nonrecursive-closure-lambda-expression u-prime))))
	       ;; See the note in new-nonrecursive-closure.
	       (or (eq? *mode* 'abstract)
		   (eq? *mode* 'symbolic)
		   (every (lambda (x v)
			   (prefix-tags? (variable-tags x) (value-tags v)))
			  (free-variables
			   (nonrecursive-closure-lambda-expression u-prime))
			  (get-nonrecursive-closure-values v)))
	       (not (some empty-abstract-value?
			  (get-nonrecursive-closure-values v)))
	       (or (eq? *mode* 'concrete)
		   (eq? *mode* 'symbolic)
		   (every (lambda (x v)
			   (some-value-tags
			    (lambda (tags)
			     (prefix-tags? (variable-tags x) tags)) v))
			  (free-variables
			   (nonrecursive-closure-lambda-expression u-prime))
			  (get-nonrecursive-closure-values v)))))
	 (set-nonrecursive-closure-canonize-cache! v u-prime)
	 (set-nonrecursive-closure-canonize-cache! u-prime u-prime)
	 (map-cps loop
		  (get-nonrecursive-closure-values v)
		  (cons (cons v u-prime) cs)
		  (lambda (vs-prime cs)
		   (fill-nonrecursive-closure-values! u-prime vs-prime)
		   (k u-prime cs)))))))
     ((recursive-closure? v)
      (cond ((recursive-closure-canonize-cache v)
	     (k (recursive-closure-canonize-cache v) cs))
	    ((deep-empty-abstract-value? v)
	     (let ((u-prime (empty-abstract-value)))
	      (k u-prime (cons (cons v u-prime) cs))))
	    (else
	     ;; See the note in abstract-environment=?.
	     (let ((u-prime (allocate-recursive-closure
			     'unfilled
			     (recursive-closure-procedure-variables v)
			     (recursive-closure-lambda-expressions v)
			     (recursive-closure-index v))))
	      (assert
	       (and
		(= (length (get-recursive-closure-values v))
		   (length (recursive-closure-variables u-prime)))
		;; See the note in new-nonrecursive-closure.
		(or (eq? *mode* 'abstract)
		    (eq? *mode* 'symbolic)
		    (every (lambda (x v)
			    (prefix-tags? (variable-tags x) (value-tags v)))
			   (recursive-closure-variables u-prime)
			   (get-recursive-closure-values v)))
		(not (some empty-abstract-value?
			   (get-recursive-closure-values v)))
		(or (eq? *mode* 'concrete)
		    (eq? *mode* 'symbolic)
		    (every (lambda (x v)
			    (some-value-tags
			     (lambda (tags)
			      (prefix-tags? (variable-tags x) tags)) v))
			   (recursive-closure-variables u-prime)
			   (get-recursive-closure-values v)))))
	      (set-recursive-closure-canonize-cache! v u-prime)
	      (set-recursive-closure-canonize-cache! u-prime u-prime)
	      (map-cps loop
		       (get-recursive-closure-values v)
		       (cons (cons v u-prime) cs)
		       (lambda (vs-prime cs)
			(fill-recursive-closure-values! u-prime vs-prime)
			(k u-prime cs)))))))
     ((perturbation-tagged-value? v)
      (cond ((perturbation-tagged-value-canonize-cache v)
	     (k (perturbation-tagged-value-canonize-cache v) cs))
	    ((deep-empty-abstract-value? v)
	     (let ((u-prime (empty-abstract-value)))
	      (k u-prime (cons (cons v u-prime) cs))))
	    (else
	     (let ((u-prime (allocate-perturbation-tagged-value 'unfilled)))
	      (assert (not (empty-abstract-value?
			    (get-perturbation-tagged-value-primal v))))
	      (set-perturbation-tagged-value-canonize-cache! v u-prime)
	      (set-perturbation-tagged-value-canonize-cache! u-prime u-prime)
	      (loop (get-perturbation-tagged-value-primal v)
		    (cons (cons v u-prime) cs)
		    (lambda (v-prime cs)
		     (fill-perturbation-tagged-value-primal! u-prime v-prime)
		     (k u-prime cs)))))))
     ((bundle? v)
      (cond
       ((bundle-canonize-cache v) (k (bundle-canonize-cache v) cs))
       ((deep-empty-abstract-value? v)
	(let ((u-prime (empty-abstract-value)))
	 (k u-prime (cons (cons v u-prime) cs))))
       (else
	(let ((u-prime (allocate-bundle 'unfilled 'unfilled)))
	 (assert
	  (and (some-bundlable? (get-bundle-primal v) (get-bundle-tangent v))
	       (not (empty-abstract-value? (get-bundle-primal v)))
	       (not (empty-abstract-value? (get-bundle-tangent v)))))
	 (set-bundle-canonize-cache! v u-prime)
	 (set-bundle-canonize-cache! u-prime u-prime)
	 (loop (get-bundle-primal v)
	       (cons (cons v u-prime) cs)
	       (lambda (v-primal-prime cs)
		(loop (get-bundle-tangent v)
		      cs
		      (lambda (v-tangent-prime cs)
		       (fill-bundle! u-prime v-primal-prime v-tangent-prime)
		       (k u-prime cs)))))))))
     ((sensitivity-tagged-value? v)
      (cond ((sensitivity-tagged-value-canonize-cache v)
	     (k (sensitivity-tagged-value-canonize-cache v) cs))
	    ((deep-empty-abstract-value? v)
	     (let ((u-prime (empty-abstract-value)))
	      (k u-prime (cons (cons v u-prime) cs))))
	    (else
	     (let ((u-prime (allocate-sensitivity-tagged-value 'unfilled)))
	      (assert (not (empty-abstract-value?
			    (get-sensitivity-tagged-value-primal v))))
	      (set-sensitivity-tagged-value-canonize-cache! v u-prime)
	      (set-sensitivity-tagged-value-canonize-cache! u-prime u-prime)
	      (loop (get-sensitivity-tagged-value-primal v)
		    (cons (cons v u-prime) cs)
		    (lambda (v-prime cs)
		     (fill-sensitivity-tagged-value-primal! u-prime v-prime)
		     (k u-prime cs)))))))
     ((reverse-tagged-value? v)
      (cond
       ((reverse-tagged-value-canonize-cache v)
	(k (reverse-tagged-value-canonize-cache v) cs))
       ((deep-empty-abstract-value? v)
	(let ((u-prime (empty-abstract-value)))
	 (k u-prime (cons (cons v u-prime) cs))))
       (else
	(let ((u-prime (allocate-reverse-tagged-value 'unfilled)))
	 (assert
	  (not (empty-abstract-value? (get-reverse-tagged-value-primal v))))
	 (set-reverse-tagged-value-canonize-cache! v u-prime)
	 (set-reverse-tagged-value-canonize-cache! u-prime u-prime)
	 (loop (get-reverse-tagged-value-primal v)
	       (cons (cons v u-prime) cs)
	       (lambda (v-prime cs)
		(fill-reverse-tagged-value-primal! u-prime v-prime)
		(k u-prime cs)))))))
     ((tagged-pair? v)
      (cond
       ((tagged-pair-canonize-cache v) (k (tagged-pair-canonize-cache v) cs))
       ((deep-empty-abstract-value? v)
	(let ((u-prime (empty-abstract-value)))
	 (k u-prime (cons (cons v u-prime) cs))))
       (else
	(let ((u-prime (allocate-tagged-pair
			(tagged-pair-tags v) 'unfilled 'unfilled)))
	 (assert
	  (and
	   (or (eq? *mode* 'abstract)
	       (eq? *mode* 'symbolic)
	       (and (prefix-tags? (tagged-pair-tags u-prime)
				  (value-tags (get-tagged-pair-car v)))
		    (prefix-tags? (tagged-pair-tags u-prime)
				  (value-tags (get-tagged-pair-cdr v)))))
	   (not (empty-abstract-value? (get-tagged-pair-car v)))
	   (not (empty-abstract-value? (get-tagged-pair-cdr v)))
	   (or
	    (eq? *mode* 'concrete)
	    (eq? *mode* 'symbolic)
	    (and
	     (some-value-tags
	      (lambda (tags) (prefix-tags? (tagged-pair-tags u-prime) tags))
	      (get-tagged-pair-car v))
	     (some-value-tags
	      (lambda (tags) (prefix-tags? (tagged-pair-tags u-prime) tags))
	      (get-tagged-pair-cdr v))))))
	 (set-tagged-pair-canonize-cache! v u-prime)
	 (set-tagged-pair-canonize-cache! u-prime u-prime)
	 (loop (get-tagged-pair-car v)
	       (cons (cons v u-prime) cs)
	       (lambda (v-car-prime cs)
		(loop (get-tagged-pair-cdr v)
		      cs
		      (lambda (v-cdr-prime cs)
		       (fill-tagged-pair! u-prime v-car-prime v-cdr-prime)
		       (k u-prime cs)))))))))
     (else (internal-error)))))))

(define (canonize-abstract-value v)
 ;; This is written in CPS so as not to break structure sharing.
 ;; The whole purpose of this procedure is to:
 ;; - propagate empty abstract values (empty unions) upward so that there are
 ;;   never any nested empty abstract values,
 ;; - to merge unions of unions so that there are never any unions immediately
 ;;   nested in another union,
 ;; - to remove singleton unions, and
 ;; - to propagate unions of transformed booleans into transformed unions of
 ;;   booleans. For bundles, this widens in the process.
 ;; If assq is replaced with assp deep-abstract-value=? then this also:
 ;; - discovers structure to share.
 ;; It is necessary to use assp deep-abstract-value=? or else an error occurs
 ;; during nr-sqrt-RR where equivalent but non-eq recursive abstract values are
 ;; nested and then path-of-greatest-depth finds a path with equivalent but
 ;; non-eq values which when merged yield a value that again has that path
 ;; causing an infinite loop.
 (time-it-bucket
  7
  (let loop ((v v) (cs '()) (k (lambda (v-prime cs) v-prime)))
   (let ((found? (assp deep-abstract-value=? v cs)))
    (cond
     (found? (k (cdr found?) cs))
     ((union? v)
      (cond
       ((union-canonize-cache v) (k (union-canonize-cache v) cs))
       ((deep-empty-abstract-value? v)
	(let ((u-prime (empty-abstract-value)))
	 (k u-prime (cons (cons v u-prime) cs))))
       (else
	;; This is the whole reason we require that abstract values be
	;; copied. This performs the optimization that unionize performs but
	;; fill-union-values! is unable to because of unfilled slots.
	(let ((us (remove-if
		   deep-empty-abstract-value?
		   ;; This is what propagates unions of transformed
		   ;; booleans into transformed unions of booleans and
		   ;; widens in the process (for bundles).
		   (union-members (reduce abstract-value-union-internal
					  (union-members v)
					  (empty-abstract-value))))))
	 ;; This is just to trigger errors on aggregate abstract values that
	 ;; have empty slots. We could do this everywhere which would trigger
	 ;; the error earlier, at the time of creation, but this just
	 ;; triggers the same error later, since we require that every
	 ;; abstract value be copied.
	 (cond ((null? us) (k (empty-abstract-value) cs))
	       ;; This used to add (cons v (first us)) to the cs cache but
	       ;; that caused a nested union bug in t22 without
	       ;; -imprecise-inexact when doing -all-limits 1
	       ;; -tagged-pair-depth-limit 3. I now believe that the
	       ;; following is correct.
	       ((null? (rest us)) (loop (first us) cs k))
	       (else (let ((v-prime (allocate-union 'unfilled)))
		      (set-union-canonize-cache! v v-prime)
		      (set-union-canonize-cache! v-prime v-prime)
		      (map-cps loop
			       us
			       (cons (cons v v-prime) cs)
			       (lambda (us-prime cs)
				(assert
				 (and (not (null? us-prime))
				      (not (null? (rest us-prime)))
				      (= (length us) (length us-prime))
				      (not (some union? us-prime))))
				(fill-union-values! v-prime us-prime)
				(k v-prime cs))))))))))
     ((vlad-empty-list? v)
      (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
     ((vlad-true? v)
      (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
     ((vlad-false? v)
      (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
     ((vlad-real? v)
      (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
     ((primitive-procedure? v)
      (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
     ((nonrecursive-closure? v)
      (cond
       ((nonrecursive-closure-canonize-cache v)
	(k (nonrecursive-closure-canonize-cache v) cs))
       ((deep-empty-abstract-value? v)
	(let ((u-prime (empty-abstract-value)))
	 (k u-prime (cons (cons v u-prime) cs))))
       (else
	;; See the note in abstract-environment=?.
	(let ((u-prime (allocate-nonrecursive-closure
			'unfilled (nonrecursive-closure-lambda-expression v))))
	 (assert
	  (and (= (length (get-nonrecursive-closure-values v))
		  (length (free-variables
			   (nonrecursive-closure-lambda-expression u-prime))))
	       ;; See the note in new-nonrecursive-closure.
	       (or (eq? *mode* 'abstract)
		   (eq? *mode* 'symbolic)
		   (every (lambda (x v)
			   (prefix-tags? (variable-tags x) (value-tags v)))
			  (free-variables
			   (nonrecursive-closure-lambda-expression u-prime))
			  (get-nonrecursive-closure-values v)))
	       (not (some empty-abstract-value?
			  (get-nonrecursive-closure-values v)))
	       (or (eq? *mode* 'concrete)
		   (eq? *mode* 'symbolic)
		   (every (lambda (x v)
			   (some-value-tags
			    (lambda (tags)
			     (prefix-tags? (variable-tags x) tags)) v))
			  (free-variables
			   (nonrecursive-closure-lambda-expression u-prime))
			  (get-nonrecursive-closure-values v)))))
	 (set-nonrecursive-closure-canonize-cache! v u-prime)
	 (set-nonrecursive-closure-canonize-cache! u-prime u-prime)
	 (map-cps loop
		  (get-nonrecursive-closure-values v)
		  (cons (cons v u-prime) cs)
		  (lambda (vs-prime cs)
		   (fill-nonrecursive-closure-values! u-prime vs-prime)
		   (k u-prime cs)))))))
     ((recursive-closure? v)
      (cond ((recursive-closure-canonize-cache v)
	     (k (recursive-closure-canonize-cache v) cs))
	    ((deep-empty-abstract-value? v)
	     (let ((u-prime (empty-abstract-value)))
	      (k u-prime (cons (cons v u-prime) cs))))
	    (else
	     ;; See the note in abstract-environment=?.
	     (let ((u-prime (allocate-recursive-closure
			     'unfilled
			     (recursive-closure-procedure-variables v)
			     (recursive-closure-lambda-expressions v)
			     (recursive-closure-index v))))
	      (assert
	       (and
		(= (length (get-recursive-closure-values v))
		   (length (recursive-closure-variables u-prime)))
		;; See the note in new-nonrecursive-closure.
		(or (eq? *mode* 'abstract)
		    (eq? *mode* 'symbolic)
		    (every (lambda (x v)
			    (prefix-tags? (variable-tags x) (value-tags v)))
			   (recursive-closure-variables u-prime)
			   (get-recursive-closure-values v)))
		(not (some empty-abstract-value?
			   (get-recursive-closure-values v)))
		(or (eq? *mode* 'concrete)
		    (eq? *mode* 'symbolic)
		    (every (lambda (x v)
			    (some-value-tags
			     (lambda (tags)
			      (prefix-tags? (variable-tags x) tags)) v))
			   (recursive-closure-variables u-prime)
			   (get-recursive-closure-values v)))))
	      (set-recursive-closure-canonize-cache! v u-prime)
	      (set-recursive-closure-canonize-cache! u-prime u-prime)
	      (map-cps loop
		       (get-recursive-closure-values v)
		       (cons (cons v u-prime) cs)
		       (lambda (vs-prime cs)
			(fill-recursive-closure-values! u-prime vs-prime)
			(k u-prime cs)))))))
     ((perturbation-tagged-value? v)
      (cond ((perturbation-tagged-value-canonize-cache v)
	     (k (perturbation-tagged-value-canonize-cache v) cs))
	    ((deep-empty-abstract-value? v)
	     (let ((u-prime (empty-abstract-value)))
	      (k u-prime (cons (cons v u-prime) cs))))
	    (else
	     (let ((u-prime (allocate-perturbation-tagged-value 'unfilled)))
	      (assert (not (empty-abstract-value?
			    (get-perturbation-tagged-value-primal v))))
	      (set-perturbation-tagged-value-canonize-cache! v u-prime)
	      (set-perturbation-tagged-value-canonize-cache! u-prime u-prime)
	      (loop (get-perturbation-tagged-value-primal v)
		    (cons (cons v u-prime) cs)
		    (lambda (v-prime cs)
		     (fill-perturbation-tagged-value-primal! u-prime v-prime)
		     (k u-prime cs)))))))
     ((bundle? v)
      (cond
       ((bundle-canonize-cache v) (k (bundle-canonize-cache v) cs))
       ((deep-empty-abstract-value? v)
	(let ((u-prime (empty-abstract-value)))
	 (k u-prime (cons (cons v u-prime) cs))))
       (else
	(let ((u-prime (allocate-bundle 'unfilled 'unfilled)))
	 (assert
	  (and (some-bundlable? (get-bundle-primal v) (get-bundle-tangent v))
	       (not (empty-abstract-value? (get-bundle-primal v)))
	       (not (empty-abstract-value? (get-bundle-tangent v)))))
	 (set-bundle-canonize-cache! v u-prime)
	 (set-bundle-canonize-cache! u-prime u-prime)
	 (loop (get-bundle-primal v)
	       (cons (cons v u-prime) cs)
	       (lambda (v-primal-prime cs)
		(loop (get-bundle-tangent v)
		      cs
		      (lambda (v-tangent-prime cs)
		       (fill-bundle! u-prime v-primal-prime v-tangent-prime)
		       (k u-prime cs)))))))))
     ((sensitivity-tagged-value? v)
      (cond ((sensitivity-tagged-value-canonize-cache v)
	     (k (sensitivity-tagged-value-canonize-cache v) cs))
	    ((deep-empty-abstract-value? v)
	     (let ((u-prime (empty-abstract-value)))
	      (k u-prime (cons (cons v u-prime) cs))))
	    (else
	     (let ((u-prime (allocate-sensitivity-tagged-value 'unfilled)))
	      (assert (not (empty-abstract-value?
			    (get-sensitivity-tagged-value-primal v))))
	      (set-sensitivity-tagged-value-canonize-cache! v u-prime)
	      (set-sensitivity-tagged-value-canonize-cache! u-prime u-prime)
	      (loop (get-sensitivity-tagged-value-primal v)
		    (cons (cons v u-prime) cs)
		    (lambda (v-prime cs)
		     (fill-sensitivity-tagged-value-primal! u-prime v-prime)
		     (k u-prime cs)))))))
     ((reverse-tagged-value? v)
      (cond
       ((reverse-tagged-value-canonize-cache v)
	(k (reverse-tagged-value-canonize-cache v) cs))
       ((deep-empty-abstract-value? v)
	(let ((u-prime (empty-abstract-value)))
	 (k u-prime (cons (cons v u-prime) cs))))
       (else
	(let ((u-prime (allocate-reverse-tagged-value 'unfilled)))
	 (assert
	  (not (empty-abstract-value? (get-reverse-tagged-value-primal v))))
	 (set-reverse-tagged-value-canonize-cache! v u-prime)
	 (set-reverse-tagged-value-canonize-cache! u-prime u-prime)
	 (loop (get-reverse-tagged-value-primal v)
	       (cons (cons v u-prime) cs)
	       (lambda (v-prime cs)
		(fill-reverse-tagged-value-primal! u-prime v-prime)
		(k u-prime cs)))))))
     ((tagged-pair? v)
      (cond
       ((tagged-pair-canonize-cache v) (k (tagged-pair-canonize-cache v) cs))
       ((deep-empty-abstract-value? v)
	(let ((u-prime (empty-abstract-value)))
	 (k u-prime (cons (cons v u-prime) cs))))
       (else
	(let ((u-prime (allocate-tagged-pair
			(tagged-pair-tags v) 'unfilled 'unfilled)))
	 (assert
	  (and
	   (or (eq? *mode* 'abstract)
	       (eq? *mode* 'symbolic)
	       (and (prefix-tags? (tagged-pair-tags u-prime)
				  (value-tags (get-tagged-pair-car v)))
		    (prefix-tags? (tagged-pair-tags u-prime)
				  (value-tags (get-tagged-pair-cdr v)))))
	   (not (empty-abstract-value? (get-tagged-pair-car v)))
	   (not (empty-abstract-value? (get-tagged-pair-cdr v)))
	   (or
	    (eq? *mode* 'concrete)
	    (eq? *mode* 'symbolic)
	    (and
	     (some-value-tags
	      (lambda (tags) (prefix-tags? (tagged-pair-tags u-prime) tags))
	      (get-tagged-pair-car v))
	     (some-value-tags
	      (lambda (tags) (prefix-tags? (tagged-pair-tags u-prime) tags))
	      (get-tagged-pair-cdr v))))))
	 (set-tagged-pair-canonize-cache! v u-prime)
	 (set-tagged-pair-canonize-cache! u-prime u-prime)
	 (loop (get-tagged-pair-car v)
	       (cons (cons v u-prime) cs)
	       (lambda (v-car-prime cs)
		(loop (get-tagged-pair-cdr v)
		      cs
		      (lambda (v-cdr-prime cs)
		       (fill-tagged-pair! u-prime v-car-prime v-cdr-prime)
		       (k u-prime cs)))))))))
     (else (internal-error)))))))

(define (intern-abstract-value v)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  8
  (let loop ((v v) (cs '()) (k (lambda (v-prime cs) v-prime)))
   (cond
    ((union? v)
     (if (union-intern-cache v)
	 (k (union-intern-cache v) cs)
	 (let ((v-prime (find-if (lambda (v-prime)
				  (filled-deep-abstract-value=? v-prime v))
				 *unions*)))
	  (if v-prime
	      (k v-prime (cons (cons v v-prime) cs))
	      (let ((v-prime (allocate-union 'unfilled)))
	       (assert (not *frozen?*))
	       (set-union-canonize-cache! v-prime v-prime)
	       (set-union-intern-cache! v v-prime)
	       (set-union-intern-cache! v-prime v-prime)
	       (map-cps
		loop
		(get-union-values v)
		(cons (cons v v-prime) cs)
		(lambda (us-prime cs)
		 (assert
		  (and (not (null? us-prime)) (not (null? (rest us-prime)))))
		 (set! *unions* (cons v-prime *unions*))
		 (fill-union-values! v-prime us-prime)
		 (k v-prime cs))))))))
    ((vlad-empty-list? v)
     (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
    ((vlad-true? v)
     (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
    ((vlad-false? v)
     (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
    ((vlad-real? v)
     (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
    ((primitive-procedure? v)
     (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
    ((nonrecursive-closure? v)
     (if (nonrecursive-closure-intern-cache v)
	 (k (nonrecursive-closure-intern-cache v) cs)
	 ;; See the notes in new-nonrecursive-closure.
	 (let ((u-prime
		(find-if (lambda (u-prime)
			  (filled-deep-abstract-value=? u-prime v))
			 (lambda-expression-nonrecursive-closures
			  (nonrecursive-closure-lambda-expression v)))))
	  (if u-prime
	      (k u-prime (cons (cons v u-prime) cs))
	      ;; See the note in abstract-environment=?.
	      (let ((u-prime
		     (allocate-nonrecursive-closure
		      'unfilled (nonrecursive-closure-lambda-expression v))))
	       (assert
		(and
		 (= (length (get-nonrecursive-closure-values v))
		    (length
		     (free-variables
		      (nonrecursive-closure-lambda-expression u-prime))))
		 ;; See the note in new-nonrecursive-closure.
		 (or (eq? *mode* 'abstract)
		     (eq? *mode* 'symbolic)
		     (every (lambda (x v)
			     (prefix-tags? (variable-tags x) (value-tags v)))
			    (free-variables
			     (nonrecursive-closure-lambda-expression u-prime))
			    (get-nonrecursive-closure-values v)))
		 (not (some empty-abstract-value?
			    (get-nonrecursive-closure-values v)))
		 (or (eq? *mode* 'concrete)
		     (eq? *mode* 'symbolic)
		     (every (lambda (x v)
			     (some-value-tags
			      (lambda (tags)
			       (prefix-tags? (variable-tags x) tags)) v))
			    (free-variables
			     (nonrecursive-closure-lambda-expression u-prime))
			    (get-nonrecursive-closure-values v)))
		 (not *frozen?*)))
	       (set-nonrecursive-closure-canonize-cache! u-prime u-prime)
	       (set-nonrecursive-closure-intern-cache! v u-prime)
	       (set-nonrecursive-closure-intern-cache! u-prime u-prime)
	       (map-cps
		loop
		(get-nonrecursive-closure-values v)
		(cons (cons v u-prime) cs)
		(lambda (vs-prime cs)
		 (set-lambda-expression-nonrecursive-closures!
		  (nonrecursive-closure-lambda-expression u-prime)
		  (cons u-prime
			(lambda-expression-nonrecursive-closures
			 (nonrecursive-closure-lambda-expression v))))
		 (fill-nonrecursive-closure-values! u-prime vs-prime)
		 (k u-prime cs))))))))
    ((recursive-closure? v)
     (if (recursive-closure-intern-cache v)
	 (k (recursive-closure-intern-cache v) cs)
	 ;; See the notes in new-recursive-closure.
	 (let ((u-prime
		(find-if (lambda (u-prime)
			  (filled-deep-abstract-value=? u-prime v))
			 (lambda-expression-recursive-closures
			  (vector-ref
			   (recursive-closure-lambda-expressions v) 0)))))
	  (if u-prime
	      (k u-prime (cons (cons v u-prime) cs))
	      ;; See the note in abstract-environment=?.
	      (let ((u-prime (allocate-recursive-closure
			      'unfilled
			      (recursive-closure-procedure-variables v)
			      (recursive-closure-lambda-expressions v)
			      (recursive-closure-index v))))
	       (assert
		(and
		 (= (length (get-recursive-closure-values v))
		    (length (recursive-closure-variables u-prime)))
		 ;; See the note in new-nonrecursive-closure.
		 (or (eq? *mode* 'abstract)
		     (eq? *mode* 'symbolic)
		     (every (lambda (x v)
			     (prefix-tags? (variable-tags x) (value-tags v)))
			    (recursive-closure-variables u-prime)
			    (get-recursive-closure-values v)))
		 (not (some empty-abstract-value?
			    (get-recursive-closure-values v)))
		 (or (eq? *mode* 'concrete)
		     (eq? *mode* 'symbolic)
		     (every (lambda (x v)
			     (some-value-tags
			      (lambda (tags)
			       (prefix-tags? (variable-tags x) tags)) v))
			    (recursive-closure-variables u-prime)
			    (get-recursive-closure-values v)))
		 (not *frozen?*)))
	       (set-recursive-closure-canonize-cache! u-prime u-prime)
	       (set-recursive-closure-intern-cache! v u-prime)
	       (set-recursive-closure-intern-cache! u-prime u-prime)
	       (map-cps
		loop
		(get-recursive-closure-values v)
		(cons (cons v u-prime) cs)
		(lambda (vs-prime cs)
		 (set-lambda-expression-recursive-closures!
		  (vector-ref (recursive-closure-lambda-expressions u-prime) 0)
		  (cons u-prime
			(lambda-expression-recursive-closures
			 (vector-ref
			  (recursive-closure-lambda-expressions v) 0))))
		 (fill-recursive-closure-values! u-prime vs-prime)
		 (k u-prime cs))))))))
    ((perturbation-tagged-value? v)
     (if (perturbation-tagged-value-intern-cache v)
	 (k (perturbation-tagged-value-intern-cache v) cs)
	 (let ((u-prime (find-if (lambda (u-prime)
				  (filled-deep-abstract-value=? u-prime v))
				 *perturbation-tagged-values*)))
	  (if u-prime
	      (k u-prime (cons (cons v u-prime) cs))
	      (let ((u-prime (allocate-perturbation-tagged-value 'unfilled)))
	       (assert (and (not (empty-abstract-value?
				  (get-perturbation-tagged-value-primal v)))
			    (not *frozen?*)))
	       (set-perturbation-tagged-value-canonize-cache! u-prime u-prime)
	       (set-perturbation-tagged-value-intern-cache! v u-prime)
	       (set-perturbation-tagged-value-intern-cache! u-prime u-prime)
	       (loop (get-perturbation-tagged-value-primal v)
		     (cons (cons v u-prime) cs)
		     (lambda (v-prime cs)
		      (set! *perturbation-tagged-values*
			    (cons u-prime *perturbation-tagged-values*))
		      (fill-perturbation-tagged-value-primal! u-prime v-prime)
		      (k u-prime cs))))))))
    ((bundle? v)
     (if (bundle-intern-cache v)
	 (k (bundle-intern-cache v) cs)
	 (let ((u-prime (find-if (lambda (u-prime)
				  (filled-deep-abstract-value=? u-prime v))
				 *bundles*)))
	  (if u-prime
	      (k u-prime (cons (cons v u-prime) cs))
	      (let ((u-prime (allocate-bundle 'unfilled 'unfilled)))
	       (assert
		(and
		 (some-bundlable? (get-bundle-primal v) (get-bundle-tangent v))
		 (not (empty-abstract-value? (get-bundle-primal v)))
		 (not (empty-abstract-value? (get-bundle-tangent v)))
		 (not *frozen?*)))
	       (set-bundle-canonize-cache! u-prime u-prime)
	       (set-bundle-intern-cache! v u-prime)
	       (set-bundle-intern-cache! u-prime u-prime)
	       (loop
		(get-bundle-primal v)
		(cons (cons v u-prime) cs)
		(lambda (v-primal-prime cs)
		 (loop (get-bundle-tangent v)
		       cs
		       (lambda (v-tangent-prime cs)
			(set! *bundles* (cons u-prime *bundles*))
			(fill-bundle! u-prime v-primal-prime v-tangent-prime)
			(k u-prime cs))))))))))
    ((sensitivity-tagged-value? v)
     (if (sensitivity-tagged-value-intern-cache v)
	 (k (sensitivity-tagged-value-intern-cache v) cs)
	 (let ((u-prime (find-if (lambda (u-prime)
				  (filled-deep-abstract-value=? u-prime v))
				 *sensitivity-tagged-values*)))
	  (if u-prime
	      (k u-prime (cons (cons v u-prime) cs))
	      (let ((u-prime (allocate-sensitivity-tagged-value 'unfilled)))
	       (assert (and (not (empty-abstract-value?
				  (get-sensitivity-tagged-value-primal v)))
			    (not *frozen?*)))
	       (set-sensitivity-tagged-value-canonize-cache! u-prime u-prime)
	       (set-sensitivity-tagged-value-intern-cache! v u-prime)
	       (set-sensitivity-tagged-value-intern-cache! u-prime u-prime)
	       (loop (get-sensitivity-tagged-value-primal v)
		     (cons (cons v u-prime) cs)
		     (lambda (v-prime cs)
		      (set! *sensitivity-tagged-values*
			    (cons u-prime *sensitivity-tagged-values*))
		      (fill-sensitivity-tagged-value-primal! u-prime v-prime)
		      (k u-prime cs))))))))
    ((reverse-tagged-value? v)
     (if (reverse-tagged-value-intern-cache v)
	 (k (reverse-tagged-value-intern-cache v) cs)
	 (let ((u-prime (find-if (lambda (u-prime)
				  (filled-deep-abstract-value=? u-prime v))
				 *reverse-tagged-values*)))
	  (if u-prime
	      (k u-prime (cons (cons v u-prime) cs))
	      (let ((u-prime (allocate-reverse-tagged-value 'unfilled)))
	       (assert
		(and
		 (not
		  (empty-abstract-value? (get-reverse-tagged-value-primal v)))
		 (not *frozen?*)))
	       (set-reverse-tagged-value-canonize-cache! u-prime u-prime)
	       (set-reverse-tagged-value-intern-cache! v u-prime)
	       (set-reverse-tagged-value-intern-cache! u-prime u-prime)
	       (loop (get-reverse-tagged-value-primal v)
		     (cons (cons v u-prime) cs)
		     (lambda (v-prime cs)
		      (set! *reverse-tagged-values*
			    (cons u-prime *reverse-tagged-values*))
		      (fill-reverse-tagged-value-primal! u-prime v-prime)
		      (k u-prime cs))))))))
    ((tagged-pair? v)
     (if (tagged-pair-intern-cache v)
	 (k (tagged-pair-intern-cache v) cs)
	 (let ((u-prime (find-if (lambda (u-prime)
				  (filled-deep-abstract-value=? u-prime v))
				 *tagged-pairs*)))
	  (if u-prime
	      (k u-prime (cons (cons v u-prime) cs))
	      (let ((u-prime (allocate-tagged-pair
			      (tagged-pair-tags v) 'unfilled 'unfilled)))
	       (assert
		(and
		 (or (eq? *mode* 'abstract)
		     (eq? *mode* 'symbolic)
		     (and (prefix-tags? (tagged-pair-tags u-prime)
					(value-tags (get-tagged-pair-car v)))
			  (prefix-tags? (tagged-pair-tags u-prime)
					(value-tags (get-tagged-pair-cdr v)))))
		 (not (empty-abstract-value? (get-tagged-pair-car v)))
		 (not (empty-abstract-value? (get-tagged-pair-cdr v)))
		 (or (eq? *mode* 'concrete)
		     (eq? *mode* 'symbolic)
		     (and (some-value-tags
			   (lambda (tags)
			    (prefix-tags? (tagged-pair-tags u-prime) tags))
			   (get-tagged-pair-car v))
			  (some-value-tags
			   (lambda (tags)
			    (prefix-tags? (tagged-pair-tags u-prime) tags))
			   (get-tagged-pair-cdr v))))
		 (not *frozen?*)))
	       (set-tagged-pair-canonize-cache! u-prime u-prime)
	       (set-tagged-pair-intern-cache! v u-prime)
	       (set-tagged-pair-intern-cache! u-prime u-prime)
	       (loop
		(get-tagged-pair-car v)
		(cons (cons v u-prime) cs)
		(lambda (v-car-prime cs)
		 (loop (get-tagged-pair-cdr v)
		       cs
		       (lambda (v-cdr-prime cs)
			(set! *tagged-pairs* (cons u-prime *tagged-pairs*))
			(fill-tagged-pair! u-prime v-car-prime v-cdr-prime)
			(k u-prime cs))))))))))
    (else (internal-error))))))

(define (canonize-and-maybe-intern-abstract-value v)
 ;; Flow analysis needs both canonized and interned representations. The
 ;; interpreter does not. I'm not sure whether interned representations must
 ;; be canonized. For now, they need not be.
 (let ((v (if *canonized?* (canonize-abstract-value v) v)))
  (if *interned?* (intern-abstract-value v) v)))

;;; Abstract Environment Equivalence

(define (abstract-environment=? vs1 vs2)
 ;; This assumes that the free variables in two alpha-equivalent expressions
 ;; are in the same order. Note that this is a weak notion of equivalence. A
 ;; stronger notion would attempt to find a correspondence between the free
 ;; variables that would allow them to be contextually alpha equivalent.
 (every abstract-value=? vs1 vs2))

;;; Search path

(define (search-include-path-without-extension pathname)
 (cond ((can-open-file-for-input? pathname) pathname)
       ((and (>= (string-length pathname) 1)
	     (char=? (string-ref pathname 0) #\/))
	(compile-time-error "Cannot find: ~a" pathname))
       (else (let loop ((include-path *include-path*))
	      (cond ((null? include-path)
		     (compile-time-error "Cannot find: ~a" pathname))
		    ((can-open-file-for-input?
		      (string-append (first include-path) "/" pathname))
		     (string-append (first include-path) "/" pathname))
		    (else (loop (rest include-path))))))))

(define (search-include-path pathname)
 (search-include-path-without-extension (default-extension pathname "vlad")))

(define (read-source pathname)
 (let ((pathname (default-extension pathname "vlad")))
  (unless (can-open-file-for-input? pathname)
   (compile-time-error "Cannot find: ~a" pathname))
  (call-with-input-file pathname
   (lambda (input-port)
    (let loop ((es '()))
     (let ((e (read input-port)))
      (cond
       ((eof-object? e) (reverse es))
       ((and (list? e)
	     (= (length e) 2)
	     (eq? (first e) 'include)
	     (string? (second e)))
	(loop
	 (append (reverse (read-source (search-include-path (second e)))) es)))
       (else (loop (cons e es))))))))))

;;; Definitions

(define (definens? e)
 (or (concrete-variable? e)
     (and (list? e) (not (null? e)) (definens? (first e)))))

(define (definition? d)
 (and
  (list? d) (= (length d) 3) (eq? (first d) 'define) (definens? (second d))))

(define (definens-name e)
 (if (concrete-variable? e) e (definens-name (first e))))

(define (definens-expression e1 e2)
 (if (concrete-variable? e1)
     e2
     (definens-expression (first e1) `(lambda ,(rest e1) ,e2))))

(define (expand-definitions ds e)
 (for-each
  (lambda (d)
   (unless (definition? d) (compile-time-error "Invalid definition: ~s" d)))
  ds)
 `(letrec ,(map (lambda (d)
		 `(,(definens-name (second d))
		   ,(definens-expression (second d) (third d))))
		ds)
   ,e))

;;; Alpha conversion

(define (alphaify x)
 (set! *alpha* (+ *alpha* 1))
 (new-variable `(alpha ,(variable-name x) ,*alpha*)))

(define (alpha-convert-parameter p)
 ;; needs work: Should have structure instead of list.
 ;; The output is (p bs) where p is the alpha converted parameter and bs is the
 ;; renamings.
 (cond
  ((constant-expression? p) (list p '()))
  ((variable-access-expression? p)
   (let ((x (alphaify (variable-access-expression-variable p))))
    (list
     (new-variable-access-expression x)
     (list (make-alpha-binding (variable-access-expression-variable p) x)))))
  ((lambda-expression? p)
   (let loop ((bs '()) (xs (parameter-variables p)))
    (if (null? xs)
	(list (alpha-convert-expression p bs) bs)
	(let ((x (alphaify (first xs))))
	 (loop (cons (make-alpha-binding (first xs) x) bs) (rest xs))))))
  ((letrec-expression? p)
   (let loop ((bs '()) (xs (parameter-variables p)))
    (if (null? xs)
	(list (alpha-convert-expression p bs) bs)
	(let ((x (alphaify (first xs))))
	 (loop (cons (make-alpha-binding (first xs) x) bs) (rest xs))))))
  ((cons-expression? p)
   (let* ((result1 (alpha-convert-parameter (cons-expression-car p)))
	  (result2 (alpha-convert-parameter (cons-expression-cdr p))))
    (list (new-cons-expression
	   (cons-expression-tags p) (first result1) (first result2))
	  (append (second result1) (second result2)))))
  (else (internal-error))))

(define (link-inverses e1 e)
 (assert
  (and
   (or (and (not (lambda-expression-alpha-conversion-inverse e))
	    (not (lambda-expression-alpha-conversion-inverse e1)))
       (and (lambda-expression-alpha-conversion-inverse e)
	    (or (not (lambda-expression-alpha-conversion-inverse e1))
		(expression-eqv?
		 (lambda-expression-alpha-conversion-inverse e)
		 (lambda-expression-alpha-conversion-inverse e1)))))
   (or (and (not (lambda-expression-perturbation-transform-inverse e))
	    (not (lambda-expression-perturbation-transform-inverse e1)))
       (and (lambda-expression-perturbation-transform-inverse e)
	    (or (not (lambda-expression-perturbation-transform-inverse e1))
		(expression-eqv?
		 (lambda-expression-perturbation-transform-inverse e)
		 (lambda-expression-perturbation-transform-inverse e1)))))
   (or (and (not (lambda-expression-forward-transform-inverse e))
	    (not (lambda-expression-forward-transform-inverse e1)))
       (and (lambda-expression-forward-transform-inverse e)
	    (or (not (lambda-expression-forward-transform-inverse e1))
		(expression-eqv?
		 (lambda-expression-forward-transform-inverse e)
		 (lambda-expression-forward-transform-inverse e1)))))
   (or (and (not (lambda-expression-sensitivity-transform-inverse e))
	    (not (lambda-expression-sensitivity-transform-inverse e1)))
       (and (lambda-expression-sensitivity-transform-inverse e)
	    (or (not (lambda-expression-sensitivity-transform-inverse e1))
		(expression-eqv?
		 (lambda-expression-sensitivity-transform-inverse e)
		 (lambda-expression-sensitivity-transform-inverse e1)))))
   (or (and (not (lambda-expression-reverse-transform-inverse e))
	    (not (lambda-expression-reverse-transform-inverse e1)))
       (and (lambda-expression-reverse-transform-inverse e)
	    (or (not (lambda-expression-reverse-transform-inverse e1))
		(expression-eqv?
		 (lambda-expression-reverse-transform-inverse e)
		 (lambda-expression-reverse-transform-inverse e1)))))))
 (when (and (lambda-expression-alpha-conversion-inverse e)
	    (not (lambda-expression-alpha-conversion-inverse e1)))
  (set-lambda-expression-alpha-conversion-inverse!
   e1 (lambda-expression-alpha-conversion-inverse e)))
 (when (and (lambda-expression-perturbation-transform-inverse e)
	    (not (lambda-expression-perturbation-transform-inverse e1)))
  (set-lambda-expression-perturbation-transform-inverse!
   e1 (lambda-expression-perturbation-transform-inverse e)))
 (when (and (lambda-expression-forward-transform-inverse e)
	    (not (lambda-expression-forward-transform-inverse e1)))
  (set-lambda-expression-forward-transform-inverse!
   e1 (lambda-expression-forward-transform-inverse e)))
 (when (and (lambda-expression-sensitivity-transform-inverse e)
	    (not (lambda-expression-sensitivity-transform-inverse e1)))
  (set-lambda-expression-sensitivity-transform-inverse!
   e1 (lambda-expression-sensitivity-transform-inverse e)))
 (when (and (lambda-expression-reverse-transform-inverse e)
	    (not (lambda-expression-reverse-transform-inverse e1)))
  (set-lambda-expression-reverse-transform-inverse!
   e1 (lambda-expression-reverse-transform-inverse e)))
 e1)

(define (alpha-convert-expression e bs)
 ;; bs is the renamings currently in scope
 ;; The output is e.
 (cond
  ((constant-expression? e) e)
  ((variable-access-expression? e)
   (new-variable-access-expression
    (alpha-binding-variable2
     (find-if (lambda (b)
	       (variable=? (alpha-binding-variable1 b)
			   (variable-access-expression-variable e)))
	      bs))))
  ((lambda-expression? e)
   (let* ((result (alpha-convert-parameter (lambda-expression-parameter e)))
	  (e1 (link-inverses
	       (new-lambda-expression
		(first result)
		(alpha-convert-expression (lambda-expression-body e)
					  (append (second result) bs)))
	       e)))
    (assert (not (lambda-expression-alpha-conversion-inverse e1)))
    (set-lambda-expression-alpha-conversion-inverse! e1 e)
    e1))
  ((application? e)
   (new-application (alpha-convert-expression (application-callee e) bs)
		    (alpha-convert-expression (application-argument e) bs)))
  ((letrec-expression? e)
   (let outer ((xs1 (letrec-expression-procedure-variables e)) (xs2 '()))
    (if (null? xs1)
	(let ((bs (append (map make-alpha-binding
			       (letrec-expression-procedure-variables e)
			       (reverse xs2))
			  bs)))
	 (let inner ((es (letrec-expression-lambda-expressions e)) (es1 '()))
	  (if (null? es)
	      (new-letrec-expression
	       (reverse xs2)
	       (reverse es1)
	       (alpha-convert-expression (letrec-expression-body e) bs))
	      (inner (rest es)
		     (cons (alpha-convert-expression (first es) bs) es1)))))
	(outer (rest xs1) (cons (alphaify (first xs1)) xs2)))))
  ((cons-expression? e)
   (new-cons-expression (cons-expression-tags e)
			(alpha-convert-expression (cons-expression-car e) bs)
			(alpha-convert-expression (cons-expression-cdr e) bs)))
  (else (internal-error))))

(define (alpha-convert e)
 (alpha-convert-expression
  e (map make-alpha-binding (free-variables e) (free-variables e))))

;;; ANF conversion

;;; The soundness of our method for ANF conversion relies on two things:
;;;  1. E must be alpha converted.
;;;     This allows letrecs to be merged.
;;;     It also allows let*s in expressions of let*s to be merged.
;;;  2. No letrec nested in a let* expression or body can reference a variable
;;;     bound by that let*.

(define (anf-result result)
 ;; needs work: Should have structure instead of list.
 (assert (or (null? (fourth result))
	     ;; needs work: To abstract this.
	     (null?
	      (rest
	       (remove-duplicates
		(map (lambda (b)
		      (lambda-expression-tags (variable-binding-expression b)))
		     (fourth result)))))))
 (new-letrec-expression
  (map variable-binding-variable (reverse (fourth result)))
  (map variable-binding-expression (reverse (fourth result)))
  (new-let* (map parameter-binding-parameter (reverse (third result)))
	    (map parameter-binding-expression (reverse (third result)))
	    (second result))))

(define (anf-max e)
 (cond
  ((constant-expression? e) 0)
  ((variable-access-expression? e)
   (variable-anf-max (variable-access-expression-variable e)))
  ((lambda-expression? e)
   (max (anf-max (lambda-expression-parameter e))
	(anf-max (lambda-expression-body e))))
  ((application? e)
   (max (anf-max (application-callee e)) (anf-max (application-argument e))))
  ((letrec-expression? e)
   (max (map-reduce
	 max 0 variable-anf-max (letrec-expression-procedure-variables e))
	(map-reduce max 0 anf-max (letrec-expression-lambda-expressions e))
	(anf-max (letrec-expression-body e))))
  ((cons-expression? e)
   (max (anf-max (cons-expression-car e)) (anf-max (cons-expression-cdr e))))
  (else (internal-error))))

(define (anf-convert-parameter i p p?)
 ;; needs work: Should have structure instead of list.
 (cond
  ;; result
  ((constant-expression? p) (list i p))
  ;; result
  ((variable-access-expression? p) (list i p))
  ((lambda-expression? p)
   (let* ((result1
	   (anf-convert-parameter i (lambda-expression-parameter p) p?))
	  (result2
	   (anf-convert-expression
	    (first result1) (lambda-expression-body p) '() '() p? #f)))
    ;; result
    (list (first result2)
	  (link-inverses
	   (new-lambda-expression (second result1) (anf-result result2)) p))))
  ((letrec-expression? p)
   (assert (and (variable-access-expression? (letrec-expression-body p))
		(memp variable=?
		      (variable-access-expression-variable
		       (letrec-expression-body p))
		      (letrec-expression-procedure-variables p))))
   (let loop ((i i) (es (letrec-expression-lambda-expressions p)) (es1 '()))
    (if (null? es)
	;; result
	(list i
	      (new-letrec-expression
	       (letrec-expression-procedure-variables p)
	       (reverse es1)
	       (letrec-expression-body p)))
	(let* ((result1 (anf-convert-parameter
			 i (lambda-expression-parameter (first es)) p?))
	       (result2 (anf-convert-expression
			 (first result1)
			 (lambda-expression-body (first es))
			 '()
			 '()
			 p?
			 #f)))
	 (loop
	  (first result2)
	  (rest es)
	  (cons (link-inverses
		 (new-lambda-expression (second result1) (anf-result result2))
		 (first es))
		es1))))))
  ((cons-expression? p)
   (let* ((result1 (anf-convert-parameter i (cons-expression-car p) p?))
	  (result2 (anf-convert-parameter
		    (first result1) (cons-expression-cdr p) p?)))
    ;; result
    (list (first result2)
	  (new-cons-expression
	   (cons-expression-tags p) (second result1) (second result2)))))
  (else (internal-error))))

(define (anf-convert-expression i e bs1 bs2 p? p1?)
 ;; needs work: Should have structure instead of list.
 (cond
  ((constant-expression? e)
   (let* ((i (+ i 1)) (p (new-variable-access-expression (anfify i))))
    ;; result
    (list i p (cons (make-parameter-binding p e) bs1) bs2)))
  ((variable-access-expression? e)
   (if p?
       ;; This is used during reverse-transform because it
       ;; guarantees that there is a one-to-one correspondence
       ;; between primal and forward phase bindings so that the
       ;; reverse transform is invertible.
       ;; result
       (list i e bs1 bs2)
       ;; This is used during parsing to guarantee that there is
       ;;                                            ___    _
       ;;                                            \      \
       ;; no binding like x = y,y which would become y,y += x
       ;; during reverse phase which incorrecty accumulates.
       (let* ((i (+ i 1)) (p (new-variable-access-expression (anfify i))))
	;; result
	(list i p (cons (make-parameter-binding p e) bs1) bs2))))
  ((lambda-expression? e)
   (if p1?
       (let* ((i (+ i 1)) (p (new-variable-access-expression (anfify i))))
	;; result
	(list i p (cons (make-parameter-binding p e) bs1) bs2))
       (let* ((result1
	       (anf-convert-parameter i (lambda-expression-parameter e) p?))
	      (result2 (anf-convert-expression (first result1)
					       (lambda-expression-body e)
					       '()
					       '()
					       p?
					       p1?))
	      (i (+ (first result2) 1))
	      (p (new-variable-access-expression (anfify i))))
	;; result
	(list
	 i
	 p
	 (cons (make-parameter-binding
		p
		(link-inverses
		 (new-lambda-expression (second result1) (anf-result result2))
		 e))
	       bs1)
	 bs2))))
  ((let*? e)
   (let* ((result1 (anf-convert-parameter
		    i (lambda-expression-parameter (application-callee e)) p?))
	  (result2 (anf-convert-reuse (second result1)
				      (first result1)
				      (application-argument e)
				      bs1
				      bs2
				      p?
				      p1?)))
    (anf-convert-expression (first result2)
			    (lambda-expression-body (application-callee e))
			    (third result2)
			    (fourth result2)
			    p?
			    p1?)))
  ((application? e)
   (let* ((result1
	   (anf-convert-expression i (application-callee e) bs1 bs2 p? p1?))
	  (result2 (anf-convert-expression (first result1)
					   (application-argument e)
					   (third result1)
					   (fourth result1)
					   p?
					   p1?))
	  (i (+ (first result2) 1))
	  (p (new-variable-access-expression (anfify i))))
    ;; result
    (list
     i
     p
     (cons (make-parameter-binding
	    p (new-application (second result1) (second result2)))
	   (third result2))
     (fourth result2))))
  ((letrec-expression? e)
   (if p1?
       (anf-convert-expression
	i
	(letrec-expression-body e)
	bs1
	(append (reverse (map make-variable-binding
			      (letrec-expression-procedure-variables e)
			      (letrec-expression-lambda-expressions e)))
		bs2)
	p?
	p1?)
       (let loop ((i i)
		  (xs (letrec-expression-procedure-variables e))
		  (es (letrec-expression-lambda-expressions e))
		  (bs2 bs2))
	(if (null? xs)
	    (anf-convert-expression
	     i (letrec-expression-body e) bs1 bs2 p? p1?)
	    (let* ((result1 (anf-convert-parameter
			     i (lambda-expression-parameter (first es)) p?))
		   (result2
		    (anf-convert-expression (first result1)
					    (lambda-expression-body (first es))
					    '()
					    '()
					    p?
					    p1?)))
	     (loop
	      (first result2)
	      (rest xs)
	      (rest es)
	      (cons
	       (make-variable-binding
		(first xs)
		(link-inverses
		 (new-lambda-expression (second result1) (anf-result result2))
		 (first es)))
	       bs2)))))))
  ((cons-expression? e)
   (let* ((result1
	   (anf-convert-expression i (cons-expression-car e) bs1 bs2 p? p1?))
	  (result2 (anf-convert-expression (first result1)
					   (cons-expression-cdr e)
					   (third result1)
					   (fourth result1)
					   p?
					   p1?))
	  (i (+ (first result2) 1))
	  (p (new-variable-access-expression (anfify i))))
    ;; result
    (list i
	  p
	  (cons (make-parameter-binding
		 p
		 (new-cons-expression
		  (cons-expression-tags e) (second result1) (second result2)))
		(third result2))
	  (fourth result2))))
  (else (internal-error))))

(define (anf-convert-reuse p i e bs1 bs2 p? p1?)
 ;; needs work: Should have structure instead of list.
 (cond
  ((constant-expression? e)
   ;; result
   (list i p (cons (make-parameter-binding p e) bs1) bs2))
  ((variable-access-expression? e)
   ;; There is copying here, since both names might be used.
   ;; result
   (list i p (cons (make-parameter-binding p e) bs1) bs2))
  ((lambda-expression? e)
   (if p1?
       ;; There is copying here, since both names might be used.
       ;; result
       (list i p (cons (make-parameter-binding p e) bs1) bs2)
       (let* ((result1
	       (anf-convert-parameter i (lambda-expression-parameter e) p?))
	      (result2 (anf-convert-expression (first result1)
					       (lambda-expression-body e)
					       '()
					       '()
					       p?
					       p1?)))
	;; result
	(list
	 (first result2)
	 p
	 (cons (make-parameter-binding
		p
		(link-inverses
		 (new-lambda-expression (second result1) (anf-result result2))
		 e))
	       bs1)
	 bs2))))
  ((let*? e)
   (let* ((result1 (anf-convert-parameter
		    i (lambda-expression-parameter (application-callee e)) p?))
	  (result2 (anf-convert-reuse (second result1)
				      (first result1)
				      (application-argument e)
				      bs1
				      bs2
				      p?
				      p1?)))
    (anf-convert-expression
     (first result2)
     (lambda-expression-body (application-callee e))
     ;; There is copying here, since both names might be used.
     (cons (make-parameter-binding p (second result1))
	   (cons (make-parameter-binding (second result1) (second result2))
		 (third result2)))
     (fourth result2)
     p?
     p1?)))
  ((application? e)
   (let* ((result1
	   (anf-convert-expression i (application-callee e) bs1 bs2 p? p1?))
	  (result2 (anf-convert-expression (first result1)
					   (application-argument e)
					   (third result1)
					   (fourth result1)
					   p?
					   p1?)))
    ;; result
    (list
     (first result2)
     p
     (cons (make-parameter-binding
	    p (new-application (second result1) (second result2)))
	   (third result2))
     (fourth result2))))
  ((letrec-expression? e)
   (if p1?
       (anf-convert-expression
	i
	(letrec-expression-body e)
	bs1
	(append (reverse (map make-variable-binding
			      (letrec-expression-procedure-variables e)
			      (letrec-expression-lambda-expressions e)))
		bs2)
	p?
	p1?)
       (let loop ((i i)
		  (xs (letrec-expression-procedure-variables e))
		  (es (letrec-expression-lambda-expressions e))
		  (bs2 bs2))
	(if (null? xs)
	    (anf-convert-expression
	     i (letrec-expression-body e) bs1 bs2 p? p1?)
	    (let* ((result1 (anf-convert-parameter
			     i (lambda-expression-parameter (first es)) p?))
		   (result2
		    (anf-convert-expression (first result1)
					    (lambda-expression-body (first es))
					    '()
					    '()
					    p?
					    p1?)))
	     (loop
	      (first result2)
	      (rest xs)
	      (rest es)
	      (cons
	       (make-variable-binding
		(first xs)
		(link-inverses
		 (new-lambda-expression (second result1) (anf-result result2))
		 (first es)))
	       bs2)))))))
  ((cons-expression? e)
   (let* ((result1
	   (anf-convert-expression i (cons-expression-car e) bs1 bs2 p? p1?))
	  (result2 (anf-convert-expression (first result1)
					   (cons-expression-cdr e)
					   (third result1)
					   (fourth result1)
					   p?
					   p1?)))
    ;; result
    (list (first result2)
	  p
	  (cons (make-parameter-binding
		 p
		 (new-cons-expression
		  (cons-expression-tags e) (second result1) (second result2)))
		(third result2))
	  (fourth result2))))
  (else (internal-error))))

(define (anf-convert e)
 (anf-result (anf-convert-expression (anf-max e) e '() '() #f #f)))

(define (anf-convert-lambda-expression e)
 (let* ((result1 (anf-convert-parameter
		  (anf-max e) (lambda-expression-parameter e) #f))
	(result2 (anf-convert-expression
		  (first result1) (lambda-expression-body e) '() '() #f #f)))
  (link-inverses
   (new-lambda-expression (second result1) (anf-result result2)) e)))

(define (anf-convert-lambda-expression-shallow e)
 (link-inverses
  (new-lambda-expression
   (lambda-expression-parameter e)
   (anf-result (anf-convert-expression
		(anf-max e) (lambda-expression-body e) '() '() #t #t)))
  e))

(define (anf-convert-lambda-expression-for-reverse e)
 (link-inverses
  (new-lambda-expression
   (lambda-expression-parameter e)
   (anf-result (anf-convert-expression
		(anf-max e) (lambda-expression-body e) '() '() #t #f)))
  e))

(define (anf-let*-parameters e)
 (if (letrec-expression? e)
     (if (let*? (letrec-expression-body e))
	 (let*-parameters (letrec-expression-body e))
	 '())
     (if (let*? e) (let*-parameters e) '())))

(define (anf-let*-expressions e)
 (if (letrec-expression? e)
     (if (let*? (letrec-expression-body e))
	 (let*-expressions (letrec-expression-body e))
	 '())
     (if (let*? e) (let*-expressions e) '())))

(define (anf-parameter e)
 (if (letrec-expression? e)
     (if (let*? (letrec-expression-body e))
	 (let*-body (letrec-expression-body e))
	 (letrec-expression-body e))
     (if (let*? e) (let*-body e) e)))

;;; Concrete->Abstract

(define (value? v)
 (or (null? v)
     (boolean? v)
     (real? v)
     (and *wizard?*
	  (perturbation-tagged-value? v)
	  (value? (get-perturbation-tagged-value-primal v)))
     (and *wizard?*
	  (bundle? v)
	  (value? (get-bundle-primal v))
	  (value? (get-bundle-tangent v)))
     (and *wizard?*
	  (sensitivity-tagged-value? v)
	  (value? (get-sensitivity-tagged-value-primal v)))
     (and *wizard?*
	  (reverse-tagged-value? v)
	  (value? (get-reverse-tagged-value-primal v)))
     (and (pair? v) (value? (car v)) (value? (cdr v)))))

(define (internalize v)
 (cond
  ((null? v) (vlad-empty-list))
  ((boolean? v) (if v (vlad-true) (vlad-false)))
  ((real? v) v)
  ((perturbation-tagged-value? v)
   (new-perturbation-tagged-value
    (internalize (get-perturbation-tagged-value-primal v))))
  ((bundle? v)
   (new-bundle
    (internalize (get-bundle-primal v)) (internalize (get-bundle-tangent v))))
  ((sensitivity-tagged-value? v)
   (new-sensitivity-tagged-value
    (internalize (get-sensitivity-tagged-value-primal v))))
  ((reverse-tagged-value? v)
   (new-reverse-tagged-value
    (internalize (get-reverse-tagged-value-primal v))))
  ((pair? v) (vlad-cons (internalize (car v)) (internalize (cdr v))))
  (else (internal-error))))

;;; needs work: To add perturb, bundle, sensitize, and *j parameters.

(define (syntax-check-parameter! p)
 (cond
  ((boolean? p) (syntax-check-parameter! `',p))
  ((real? p) (syntax-check-parameter! `',p))
  ((concrete-variable? p)
   (unless (or (concrete-user-variable? p) *wizard?*)
    (compile-time-error "Invalid parameter: ~s" p))
   #f)
  ((and (list? p) (not (null? p)))
   (case (first p)
    ((quote) (unless (and (= (length p) 2) (value? (second p)))
	      (compile-time-error "Invalid parameter: ~s" p))
	     #f)
    ((cons)
     (unless (= (length p) 3) (compile-time-error "Invalid parameter: ~s" p))
     (syntax-check-parameter! (second p))
     (syntax-check-parameter! (third p)))
    ((cons*) (syntax-check-parameter! (macro-expand p)))
    ((list) (syntax-check-parameter! (macro-expand p)))
    (else (compile-time-error "Invalid parameter: ~s" p))))
  (else (compile-time-error "Invalid parameter: ~s" p))))

(define (macro-expand e)
 (if (and (list? e) (not (null? e)))
     (case (first e)
      ((lambda) (unless (and (= (length e) 3) (list? (second e)))
		 (compile-time-error "Invalid expression: ~s" e))
		(case (length (second e))
		 ((0) `(lambda ((cons* ,@(second e))) ,(third e)))
		 ((1) e)
		 (else `(lambda ((cons* ,@(second e))) ,(third e)))))
      ((let) (unless (and (= (length e) 3)
			  (list? (second e))
			  (every (lambda (b) (and (list? b) (= (length b) 2)))
				 (second e)))
	      (compile-time-error "Invalid expression: ~s" e))
	     `((lambda ,(map first (second e)) ,(third e))
	       ,@(map second (second e))))
      ((let*)
       (unless (and (= (length e) 3)
		    (list? (second e))
		    (every (lambda (b) (and (list? b) (= (length b) 2)))
			   (second e)))
	(compile-time-error "Invalid expression: ~s" e))
       (case (length (second e))
	((0) (third e))
	((1) `(let ,(second e) ,(third e)))
	(else
	 `(let (,(first (second e))) (let* ,(rest (second e)) ,(third e))))))
      ((if)
       (unless (= (length e) 4)
	(compile-time-error "Invalid expression: ~s" e))
       ;; needs work: To ensure that you don't shadow if-procedure.
       `(if-procedure
	 ,(second e) (lambda () ,(third e)) (lambda () ,(fourth e))))
      ((cons*) (case (length (rest e))
		((0) ''())
		((1) (second e))
		(else `(cons ,(second e) (cons* ,@(rest (rest e)))))))
      ((list)
       (if (null? (rest e)) ''() `(cons ,(second e) (list ,@(rest (rest e))))))
      ;; We don't allow (cond ... (e) ...) or (cond ... (e1 => e2) ...).
      ((cond) (unless (and (>= (length e) 2)
			   (every (lambda (b) (and (list? b) (= (length b) 2)))
				  (rest e))
			   (eq? (first (last e)) 'else))
	       (compile-time-error "Invalid expression: ~s" e))
	      (if (null? (rest (rest e)))
		  (second (second e))
		  `(if ,(first (second e))
		       ,(second (second e))
		       (cond ,@(rest (rest e))))))
      ((and) (case (length (rest e))
	      ((0) #t)
	      ((1) (second e))
	      (else `(if ,(second e) (and ,@(rest (rest e))) #f))))
      ((or) (case (length (rest e))
	     ((0) #f)
	     ((1) (second e))
	     (else
	      (let ((x (gensym)))
	       `(let ((,x ,(second e))) (if ,x ,x (or ,@(rest (rest e)))))))))
      (else (case (length (rest e))
	     ((0) `(,(first e) (cons* ,@(rest e))))
	     ((1) e)
	     (else `(,(first e) (cons* ,@(rest e)))))))
     e))

(define (syntax-check-expression! e)
 (let loop ((e e) (xs (map value-binding-variable *value-bindings*)))
  (cond
   ((boolean? e) (loop `',e xs))
   ((real? e) (loop `',e xs))
   ((concrete-variable? e)
    (unless (memp variable=? (new-variable e) xs)
     (compile-time-error "Unbound variable: ~s" e))
    #f)
   ((and (list? e) (not (null? e)))
    (case (first e)
     ((quote) (unless (and (= (length e) 2) (value? (second e)))
	       (compile-time-error "Invalid expression: ~s" e))
	      #f)
     ((lambda)
      (unless (and (= (length e) 3) (list? (second e)))
       (compile-time-error "Invalid expression: ~s" e))
      (case (length (second e))
       ((0) (loop (macro-expand e) xs))
       ((1) (syntax-check-parameter! (first (second e)))
	    (let ((xs0 (parameter-variables
			(internalize-expression (first (second e))))))
	     (when (duplicatesp? variable=? xs0)
	      (compile-time-error "Duplicate variables: ~s" e))
	     (loop (third e) (append xs0 xs))))
       (else (loop (macro-expand e) xs))))
     ((letrec)
      (unless (and (= (length e) 3)
		   (list? (second e))
		   (every
		    (lambda (b)
		     (and (list? b)
			  (= (length b) 2) (concrete-variable? (first b))))
		    (second e)))
       (compile-time-error "Invalid expression: ~s" e))
      (let ((xs0 (map (lambda (b) (new-variable (first b))) (second e))))
       (when (duplicatesp? variable=? xs0)
	(compile-time-error "Duplicate variables: ~s" e))
       (for-each
	(lambda (b)
	 (let ((e1 (macro-expand (second b))))
	  (unless (and (list? e1) (= (length e1) 3) (eq? (first e1) 'lambda))
	   (compile-time-error "Invalid expression: ~s" e))
	  (loop e1 (append xs0 xs))))
	(second e))
       (loop (third e) (append xs0 xs))))
     ((let) (loop (macro-expand e) xs))
     ((let*) (loop (macro-expand e) xs))
     ((if) (loop (macro-expand e) xs))
     ((cons)
      (unless (= (length e) 3) (compile-time-error "Invalid expression: ~s" e))
      (loop (second e) xs)
      (loop (third e) xs))
     ((cons*) (loop (macro-expand e) xs))
     ((list) (loop (macro-expand e) xs))
     ((cond) (loop (macro-expand e) xs))
     ((and) (loop (macro-expand e) xs))
     ((or) (loop (macro-expand e) xs))
     (else (case (length (rest e))
	    ((0) (loop (macro-expand e) xs))
	    ((1) (loop (first e) xs)
		 (loop (second e) xs))
	    (else (loop (macro-expand e) xs))))))
   (else (compile-time-error "Invalid expression: ~s" e)))))

(define (internalize-expression e)
 (cond
  ((boolean? e) (internalize-expression `',e))
  ((real? e) (internalize-expression `',e))
  ((concrete-variable? e) (new-variable-access-expression (new-variable e)))
  ((and (list? e) (not (null? e)))
   (case (first e)
    ((quote) (new-constant-expression (internalize (second e))))
    ((lambda)
     (case (length (second e))
      ((0) (internalize-expression (macro-expand e)))
      ((1) (new-lambda-expression (internalize-expression (first (second e)))
				  (internalize-expression (third e))))
      (else (internalize-expression (macro-expand e)))))
    ((letrec)
     (create-letrec-expression
      (map (lambda (b) (new-variable (first b))) (second e))
      (map (lambda (b) (internalize-expression (macro-expand (second b))))
	   (second e))
      (internalize-expression (third e))))
    ((let) (internalize-expression (macro-expand e)))
    ((let*) (internalize-expression (macro-expand e)))
    ((if) (internalize-expression (macro-expand e)))
    ((cons) (create-cons-expression (internalize-expression (second e))
				    (internalize-expression (third e))))
    ((cons*) (internalize-expression (macro-expand e)))
    ((list) (internalize-expression (macro-expand e)))
    ((cond) (internalize-expression (macro-expand e)))
    ((and) (internalize-expression (macro-expand e)))
    ((or) (internalize-expression (macro-expand e)))
    (else (case (length (rest e))
	   ((0) (internalize-expression (macro-expand e)))
	   ((1) (new-application (internalize-expression (first e))
				 (internalize-expression (second e))))
	   (else (internalize-expression (macro-expand e)))))))
  (else (internal-error))))

(define (parse e)
 (let ((e (anf-convert (alpha-convert (internalize-expression e)))))
  (list e
	(map (lambda (x)
	      (find-if (lambda (b) (variable=? x (value-binding-variable b)))
		       *value-bindings*))
	     (free-variables e)))))

;;; AD

(define (abstractify v)
 ;; This is written in CPS so as not to break structure sharing.
 ;; needs work: Should cache and eliminate cs.
 (let loop ((v v)
	    (cs '())
	    (k (lambda (v cs)
		(with-abstract
		 (lambda () (canonize-and-maybe-intern-abstract-value v))))))
  (cond
   ((unit? v) (k (unit-abstract-value v) cs))
   ;; need work: To use => here and elsewhere.
   ((assq v cs) (k (cdr (assq v cs)) cs))
   ((union? v)
    (let ((v-prime (allocate-union 'unfilled)))
     (map-cps loop
	      (union-members v)
	      (cons (cons v v-prime) cs)
	      (lambda (us-prime cs)
	       (fill-union-values! v-prime us-prime)
	       (k v-prime cs)))))
   ((vlad-empty-list? v)
    (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
   ((vlad-true? v)
    (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
   ((vlad-false? v)
    (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
   ((vlad-real? v)
    (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
   ((primitive-procedure? v)
    (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
   ((nonrecursive-closure? v)
    ;; See the note in abstract-environment=?.
    (let ((u-prime (allocate-nonrecursive-closure
		    'unfilled (nonrecursive-closure-lambda-expression v))))
     (map-cps loop
	      (get-nonrecursive-closure-values v)
	      (cons (cons v u-prime) cs)
	      (lambda (vs-prime cs)
	       (fill-nonrecursive-closure-values! u-prime vs-prime)
	       (k u-prime cs)))))
   ((recursive-closure? v)
    ;; See the note in abstract-environment=?.
    (let ((u-prime (allocate-recursive-closure
		    'unfilled
		    (recursive-closure-procedure-variables v)
		    (recursive-closure-lambda-expressions v)
		    (recursive-closure-index v))))
     (map-cps loop
	      (get-recursive-closure-values v)
	      (cons (cons v u-prime) cs)
	      (lambda (vs-prime cs)
	       (fill-recursive-closure-values! u-prime vs-prime)
	       (k u-prime cs)))))
   ((perturbation-tagged-value? v)
    (let ((u-prime (allocate-perturbation-tagged-value 'unfilled)))
     (loop (get-perturbation-tagged-value-primal v)
	   (cons (cons v u-prime) cs)
	   (lambda (v-prime cs)
	    (fill-perturbation-tagged-value-primal! u-prime v-prime)
	    (k u-prime cs)))))
   ((bundle? v)
    (let ((u-prime (allocate-bundle 'unfilled 'unfilled)))
     (loop (get-bundle-primal v)
	   (cons (cons v u-prime) cs)
	   (lambda (v-primal-prime cs)
	    (loop (get-bundle-tangent v)
		  cs
		  (lambda (v-tangent-prime cs)
		   (fill-bundle! u-prime v-primal-prime v-tangent-prime)
		   (k u-prime cs)))))))
   ((sensitivity-tagged-value? v)
    (let ((u-prime (allocate-sensitivity-tagged-value 'unfilled)))
     (loop (get-sensitivity-tagged-value-primal v)
	   (cons (cons v u-prime) cs)
	   (lambda (v-prime cs)
	    (fill-sensitivity-tagged-value-primal! u-prime v-prime)
	    (k u-prime cs)))))
   ((reverse-tagged-value? v)
    (let ((u-prime (allocate-reverse-tagged-value 'unfilled)))
     (loop (get-reverse-tagged-value-primal v)
	   (cons (cons v u-prime) cs)
	   (lambda (v-prime cs)
	    (fill-reverse-tagged-value-primal! u-prime v-prime)
	    (k u-prime cs)))))
   ((tagged-pair? v)
    (let ((u-prime (allocate-tagged-pair
		    (tagged-pair-tags v) 'unfilled 'unfilled)))
     (loop (get-tagged-pair-car v)
	   (cons (cons v u-prime) cs)
	   (lambda (v-car-prime cs)
	    (loop (get-tagged-pair-cdr v)
		  cs
		  (lambda (v-cdr-prime cs)
		   (fill-tagged-pair! u-prime v-car-prime v-cdr-prime)
		   (k u-prime cs)))))))
   (else (internal-error)))))

(define (zero v)
 (when (and *expensive-checks?* *interned?*)
  (check-canonize-cache! v)
  (check-intern-cache! v)
  (check-interned! v))
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  9
  (let loop ((v v) (top? #t) (k canonize-and-maybe-intern-abstract-value))
   (cond
    ((and
      (eq? *mode* 'symbolic) (not top?) (not (inline-zero? (abstractify v))))
     (let ((v-abstract (abstractify v)))
      (k (new-call-unit (with-abstract (lambda () (zero v-abstract)))
			(c:builtin-name "zero" v-abstract)
			v))))
    ((unit? v)
     (if (vlad-real? (unit-abstract-value v)) (k 0) (loop (unroll v) top? k)))
    ((union? v)
     (if (union-zero-cache v)
	 (k (union-zero-cache v))
	 (let ((v0 (create-tagged-union (union-tag v) 'unfilled)))
	  (set-union-zero-cache! v v0)
	  (set-union-zero-cache! v0 v0)
	  (map-cps-non-cs (lambda (v k) (loop v #f k))
			  (union-members v)
			  (lambda (us0)
			   (fill-union-values! v0 us0)
			   (k v0))))))
    ((vlad-empty-list? v) (k v))
    ((vlad-true? v) (k v))
    ((vlad-false? v) (k v))
    ((vlad-real? v) (k 0))
    ((primitive-procedure? v) (k v))
    ((nonrecursive-closure? v)
     (if (nonrecursive-closure-zero-cache v)
	 (k (nonrecursive-closure-zero-cache v))
	 ;; See the note in abstract-environment=?.
	 (let ((u0 (allocate-nonrecursive-closure
		    'unfilled (nonrecursive-closure-lambda-expression v))))
	  (set-nonrecursive-closure-zero-cache! v u0)
	  (set-nonrecursive-closure-zero-cache! u0 u0)
	  (map-cps-non-cs (lambda (v k) (loop v #f k))
			  (get-nonrecursive-closure-values v)
			  (lambda (vs0)
			   (fill-nonrecursive-closure-values! u0 vs0)
			   (k u0))))))
    ((recursive-closure? v)
     (if (recursive-closure-zero-cache v)
	 (k (recursive-closure-zero-cache v))
	 ;; See the note in abstract-environment=?.
	 (let ((u0 (allocate-recursive-closure
		    'unfilled
		    (recursive-closure-procedure-variables v)
		    (recursive-closure-lambda-expressions v)
		    (recursive-closure-index v))))
	  (set-recursive-closure-zero-cache! v u0)
	  (set-recursive-closure-zero-cache! u0 u0)
	  (map-cps-non-cs (lambda (v k) (loop v #f k))
			  (get-recursive-closure-values v)
			  (lambda (vs0)
			   (fill-recursive-closure-values! u0 vs0)
			   (k u0))))))
    ((perturbation-tagged-value? v)
     (if (perturbation-tagged-value-zero-cache v)
	 (k (perturbation-tagged-value-zero-cache v))
	 (let ((u0 (allocate-perturbation-tagged-value 'unfilled)))
	  (set-perturbation-tagged-value-zero-cache! v u0)
	  (set-perturbation-tagged-value-zero-cache! u0 u0)
	  (loop (get-perturbation-tagged-value-primal v)
		#f
		(lambda (v0)
		 (fill-perturbation-tagged-value-primal! u0 v0)
		 (k u0))))))
    ((bundle? v)
     (if (bundle-zero-cache v)
	 (k (bundle-zero-cache v))
	 (let ((u0 (allocate-bundle 'unfilled 'unfilled)))
	  (set-bundle-zero-cache! v u0)
	  (set-bundle-zero-cache! u0 u0)
	  (loop (get-bundle-primal v)
		#f
		(lambda (v0-primal)
		 (loop (get-bundle-tangent v)
		       #f
		       (lambda (v0-tangent)
			(fill-bundle! u0 v0-primal v0-tangent)
			(k u0))))))))
    ((sensitivity-tagged-value? v)
     (if (sensitivity-tagged-value-zero-cache v)
	 (k (sensitivity-tagged-value-zero-cache v))
	 (let ((u0 (allocate-sensitivity-tagged-value 'unfilled)))
	  (set-sensitivity-tagged-value-zero-cache! v u0)
	  (set-sensitivity-tagged-value-zero-cache! u0 u0)
	  (loop (get-sensitivity-tagged-value-primal v)
		#f
		(lambda (v0)
		 (fill-sensitivity-tagged-value-primal! u0 v0)
		 (k u0))))))
    ((reverse-tagged-value? v)
     (if (reverse-tagged-value-zero-cache v)
	 (k (reverse-tagged-value-zero-cache v))
	 (let ((u0 (allocate-reverse-tagged-value 'unfilled)))
	  (set-reverse-tagged-value-zero-cache! v u0)
	  (set-reverse-tagged-value-zero-cache! u0 u0)
	  (loop (get-reverse-tagged-value-primal v)
		#f
		(lambda (v0)
		 (fill-reverse-tagged-value-primal! u0 v0)
		 (k u0))))))
    ((tagged-pair? v)
     (if (tagged-pair-zero-cache v)
	 (k (tagged-pair-zero-cache v))
	 (let ((u0 (allocate-tagged-pair
		    (tagged-pair-tags v) 'unfilled 'unfilled)))
	  (set-tagged-pair-zero-cache! v u0)
	  (set-tagged-pair-zero-cache! u0 u0)
	  (loop (get-tagged-pair-car v)
		#f
		(lambda (v0-car)
		 (loop (get-tagged-pair-cdr v)
		       #f
		       (lambda (v0-cdr)
			(fill-tagged-pair! u0 v0-car v0-cdr)
			(k u0))))))))
    (else (internal-error))))))

;;; Forward Mode

(define (perturbation-transform e)
 (define (loop e)
  (cond ((constant-expression? e)
	 (with-concrete
	  (lambda ()
	   (new-constant-expression (perturb (constant-expression-value e))))))
	((variable-access-expression? e) (perturbationify-access e))
	((lambda-expression? e) (perturbation-transform e))
	((application? e)
	 (new-application (loop (application-callee e))
			  (loop (application-argument e))))
	((letrec-expression? e)
	 (new-letrec-expression
	  (map perturbationify (letrec-expression-procedure-variables e))
	  (map loop (letrec-expression-lambda-expressions e))
	  (loop (letrec-expression-body e))))
	((cons-expression? e)
	 (new-cons-expression (add-tag 'perturbation (cons-expression-tags e))
			      (loop (cons-expression-car e))
			      (loop (cons-expression-cdr e))))
	(else (internal-error))))
 (assert (lambda-expression? e))
 (if (lambda-expression-perturbation-transform e)
     (lambda-expression-perturbation-transform e)
     (let ((e1 (new-lambda-expression (loop (lambda-expression-parameter e))
				      (loop (lambda-expression-body e)))))
      (assert
       (and (not (lambda-expression-perturbation-transform e))
	    (not (lambda-expression-perturbation-transform-inverse e1))))
      (set-lambda-expression-perturbation-transform! e e1)
      (set-lambda-expression-perturbation-transform-inverse! e1 e)
      e1)))

(define (perturbation-transform-inverse e)
 (assert (and (lambda-expression? e)
	      (lambda-expression-perturbation-transform-inverse e)))
 (lambda-expression-perturbation-transform-inverse e))

(define (forward-transform e)
 (define (loop e)
  (cond
   ((constant-expression? e)
    (with-concrete
     (lambda () (new-constant-expression (j* (constant-expression-value e))))))
   ((variable-access-expression? e) (forwardify-access e))
   ((lambda-expression? e) (forward-transform e))
   ((application? e)
    (new-application (loop (application-callee e))
		     (loop (application-argument e))))
   ((letrec-expression? e)
    (new-letrec-expression
     (map forwardify (letrec-expression-procedure-variables e))
     (map loop (letrec-expression-lambda-expressions e))
     (loop (letrec-expression-body e))))
   ((cons-expression? e)
    (new-cons-expression (add-tag 'forward (cons-expression-tags e))
			 (loop (cons-expression-car e))
			 (loop (cons-expression-cdr e))))
   (else (internal-error))))
 (assert (lambda-expression? e))
 (if (lambda-expression-forward-transform e)
     (lambda-expression-forward-transform e)
     (let ((e1 (new-lambda-expression (loop (lambda-expression-parameter e))
				      (loop (lambda-expression-body e)))))
      (assert (and (not (lambda-expression-forward-transform e))
		   (not (lambda-expression-forward-transform-inverse e1))))
      (set-lambda-expression-forward-transform! e e1)
      (set-lambda-expression-forward-transform-inverse! e1 e)
      e1)))

(define (forward-transform-inverse e)
 (assert (and (lambda-expression? e)
	      (lambda-expression-forward-transform-inverse e)))
 (lambda-expression-forward-transform-inverse e))

(define (perturb v)
 ;; This is written in CPS so as not to break structure sharing.
 (when (and *expensive-checks?* *interned?*)
  (check-canonize-cache! v)
  (check-intern-cache! v)
  (check-interned! v))
 (time-it-bucket
  10
  (let loop ((v v) (top? #t) (k canonize-and-maybe-intern-abstract-value))
   (cond
    ((and (eq? *mode* 'symbolic)
	  (not top?)
	  (not (inline-perturb? (abstractify v))))
     (let ((v-abstract (abstractify v)))
      (k (new-call-unit (with-abstract (lambda () (perturb v-abstract)))
			(c:builtin-name "perturb" v-abstract)
			v))))
    ((unit? v)
     (if (vlad-real? (unit-abstract-value v))
	 (k (new-perturbation-tagged-value v))
	 (loop (unroll v) top? k)))
    ((union? v)
     (if (union-perturb-cache v)
	 (k (union-perturb-cache v))
	 (let ((v-perturbation (create-tagged-union (union-tag v) 'unfilled)))
	  (set-union-unperturb-cache! v-perturbation v)
	  (set-union-perturb-cache! v v-perturbation)
	  (map-cps-non-cs (lambda (v k) (loop v #f k))
			  (union-members v)
			  (lambda (us-perturbation)
			   (fill-union-values! v-perturbation us-perturbation)
			   (k v-perturbation))))))
    ((vlad-empty-list? v) (k (new-perturbation-tagged-value v)))
    ((vlad-true? v) (k (new-perturbation-tagged-value v)))
    ((vlad-false? v) (k (new-perturbation-tagged-value v)))
    ((vlad-real? v) (k (new-perturbation-tagged-value v)))
    ((primitive-procedure? v) (k (new-perturbation-tagged-value v)))
    ((nonrecursive-closure? v)
     (if (nonrecursive-closure-perturb-cache v)
	 (k (nonrecursive-closure-perturb-cache v))
	 ;; See the note in abstract-environment=?.
	 (let ((u-perturbation
		(allocate-nonrecursive-closure
		 'unfilled
		 (perturbation-transform
		  (nonrecursive-closure-lambda-expression v)))))
	  (set-nonrecursive-closure-unperturb-cache! u-perturbation v)
	  (set-nonrecursive-closure-perturb-cache! v u-perturbation)
	  (map-cps-non-cs
	   (lambda (v k) (loop v #f k))
	   (get-nonrecursive-closure-values v)
	   (lambda (vs-perturbation)
	    (fill-nonrecursive-closure-values! u-perturbation vs-perturbation)
	    (k u-perturbation))))))
    ((recursive-closure? v)
     (if (recursive-closure-perturb-cache v)
	 (k (recursive-closure-perturb-cache v))
	 ;; See the note in abstract-environment=?.
	 (let ((u-perturbation
		(allocate-recursive-closure
		 'unfilled
		 (map-vector perturbationify
			     (recursive-closure-procedure-variables v))
		 (map-vector perturbation-transform
			     (recursive-closure-lambda-expressions v))
		 (recursive-closure-index v))))
	  (set-recursive-closure-unperturb-cache! u-perturbation v)
	  (set-recursive-closure-perturb-cache! v u-perturbation)
	  (map-cps-non-cs
	   (lambda (v k) (loop v #f k))
	   (get-recursive-closure-values v)
	   (lambda (vs-perturbation)
	    (fill-recursive-closure-values! u-perturbation vs-perturbation)
	    (k u-perturbation))))))
    ((perturbation-tagged-value? v)
     (if (perturbation-tagged-value-perturb-cache v)
	 (k (perturbation-tagged-value-perturb-cache v))
	 (let ((u-perturbation (create-perturbation-tagged-value v)))
	  (set-perturbation-tagged-value-perturb-cache! v u-perturbation)
	  (k u-perturbation))))
    ((bundle? v)
     (if (bundle-perturb-cache v)
	 (k (bundle-perturb-cache v))
	 (let ((u-perturbation (create-perturbation-tagged-value v)))
	  (set-bundle-perturb-cache! v u-perturbation)
	  (k u-perturbation))))
    ((sensitivity-tagged-value? v)
     (if (sensitivity-tagged-value-perturb-cache v)
	 (k (sensitivity-tagged-value-perturb-cache v))
	 (let ((u-perturbation (create-perturbation-tagged-value v)))
	  (set-sensitivity-tagged-value-perturb-cache! v u-perturbation)
	  (k u-perturbation))))
    ((reverse-tagged-value? v)
     (if (reverse-tagged-value-perturb-cache v)
	 (k (reverse-tagged-value-perturb-cache v))
	 (let ((u-perturbation (create-perturbation-tagged-value v)))
	  (set-reverse-tagged-value-perturb-cache! v u-perturbation)
	  (k u-perturbation))))
    ((tagged-pair? v)
     (if (tagged-pair-perturb-cache v)
	 (k (tagged-pair-perturb-cache v))
	 (let ((u-perturbation (allocate-tagged-pair
				(add-tag 'perturbation (tagged-pair-tags v))
				'unfilled
				'unfilled)))
	  (set-tagged-pair-unperturb-cache! u-perturbation v)
	  (set-tagged-pair-perturb-cache! v u-perturbation)
	  (loop (get-tagged-pair-car v)
		#f
		(lambda (v-car-perturbation)
		 (loop (get-tagged-pair-cdr v)
		       #f
		       (lambda (v-cdr-perturbation)
			(fill-tagged-pair!
			 u-perturbation v-car-perturbation v-cdr-perturbation)
			(k u-perturbation))))))))
    (else (internal-error))))))

(define (unperturb v-perturbation)
 ;; This is written in CPS so as not to break structure sharing.
 (when (and *expensive-checks?* *interned?*)
  (check-canonize-cache! v-perturbation)
  (check-intern-cache! v-perturbation)
  (check-interned! v-perturbation))
 (time-it-bucket
  11
  (let loop ((v-perturbation v-perturbation)
	     (top? #t)
	     (k canonize-and-maybe-intern-abstract-value))
   (cond
    ((and (eq? *mode* 'symbolic)
	  (not top?)
	  (not (inline-unperturb? (abstractify v-perturbation))))
     (let ((v-perturbation-abstract (abstractify v-perturbation)))
      (k (new-call-unit
	  (with-abstract (lambda () (unperturb v-perturbation-abstract)))
	  (c:builtin-name "unperturb" v-perturbation-abstract)
	  v-perturbation))))
    ((unit? v-perturbation)
     (if (vlad-real? (unit-abstract-value v-perturbation))
	 (k (new-panic-unit
	     "Argument to unperturb is not a perturbation value"))
	 (loop (unroll v-perturbation) top? k)))
    ((union? v-perturbation)
     (if (union-unperturb-cache v-perturbation)
	 (k (union-unperturb-cache v-perturbation))
	 (let ((v (create-tagged-union (union-tag v-perturbation) 'unfilled)))
	  (set-union-perturb-cache! v v-perturbation)
	  (set-union-unperturb-cache! v-perturbation v)
	  (map-cps-non-cs (lambda (v k) (loop v #f k))
			  (union-members v-perturbation)
			  (lambda (us)
			   (fill-union-values! v us)
			   (k v))))))
    ((vlad-empty-list? v-perturbation)
     (k (ad-error "Argument to unperturb ~a a perturbation value"
		  v-perturbation)))
    ((vlad-true? v-perturbation)
     (k (ad-error "Argument to unperturb ~a a perturbation value"
		  v-perturbation)))
    ((vlad-false? v-perturbation)
     (k (ad-error "Argument to unperturb ~a a perturbation value"
		  v-perturbation)))
    ((vlad-real? v-perturbation)
     (k (ad-error "Argument to unperturb ~a a perturbation value"
		  v-perturbation)))
    ((primitive-procedure? v-perturbation)
     (k (ad-error "Argument to unperturb ~a a perturbation value"
		  v-perturbation)))
    ((nonrecursive-closure? v-perturbation)
     (cond
      ((nonrecursive-closure-unperturb-cache v-perturbation)
       (k (nonrecursive-closure-unperturb-cache v-perturbation)))
      ((tagged? 'perturbation (nonrecursive-closure-tags v-perturbation))
       ;; See the note in abstract-environment=?.
       (let ((u (allocate-nonrecursive-closure
		 'unfilled
		 (perturbation-transform-inverse
		  (nonrecursive-closure-lambda-expression v-perturbation)))))
	(set-nonrecursive-closure-perturb-cache! u v-perturbation)
	(set-nonrecursive-closure-unperturb-cache! v-perturbation u)
	(map-cps-non-cs (lambda (v k) (loop v #f k))
			(get-nonrecursive-closure-values v-perturbation)
			(lambda (vs)
			 (fill-nonrecursive-closure-values! u vs)
			 (k u)))))
      (else (k (ad-error "Argument to unperturb ~a a perturbation value"
			 v-perturbation)))))
    ((recursive-closure? v-perturbation)
     (cond ((recursive-closure-unperturb-cache v-perturbation)
	    (k (recursive-closure-unperturb-cache v-perturbation)))
	   ((tagged? 'perturbation (recursive-closure-tags v-perturbation))
	    ;; See the note in abstract-environment=?.
	    (let ((u (allocate-recursive-closure
		      'unfilled
		      (map-vector
		       unperturbationify
		       (recursive-closure-procedure-variables v-perturbation))
		      (map-vector
		       perturbation-transform-inverse
		       (recursive-closure-lambda-expressions v-perturbation))
		      (recursive-closure-index v-perturbation))))
	     (set-recursive-closure-perturb-cache! u v-perturbation)
	     (set-recursive-closure-unperturb-cache! v-perturbation u)
	     (map-cps-non-cs (lambda (v k) (loop v #f k))
			     (get-recursive-closure-values v-perturbation)
			     (lambda (vs)
			      (fill-recursive-closure-values! u vs)
			      (k u)))))
	   (else (k (ad-error "Argument to unperturb ~a a perturbation value"
			      v-perturbation)))))
    ((perturbation-tagged-value? v-perturbation)
     (k (get-perturbation-tagged-value-primal v-perturbation)))
    ((bundle? v-perturbation)
     (k (ad-error "Argument to unperturb ~a a perturbation value"
		  v-perturbation)))
    ((sensitivity-tagged-value? v-perturbation)
     (k (ad-error "Argument to unperturb ~a a perturbation value"
		  v-perturbation)))
    ((reverse-tagged-value? v-perturbation)
     (k (ad-error "Argument to unperturb ~a a perturbation value"
		  v-perturbation)))
    ((tagged-pair? v-perturbation)
     (cond
      ((tagged-pair-unperturb-cache v-perturbation)
       (k (tagged-pair-unperturb-cache v-perturbation)))
      ((tagged? 'perturbation (tagged-pair-tags v-perturbation))
       (let ((u (allocate-tagged-pair
		 (remove-tag 'perturbation (tagged-pair-tags v-perturbation))
		 'unfilled
		 'unfilled)))
	(set-tagged-pair-perturb-cache! u v-perturbation)
	(set-tagged-pair-unperturb-cache! v-perturbation u)
	(loop (get-tagged-pair-car v-perturbation)
	      #f
	      (lambda (v-car)
	       (loop (get-tagged-pair-cdr v-perturbation)
		     #f
		     (lambda (v-cdr)
		      (fill-tagged-pair! u v-car v-cdr)
		      (k u)))))))
      (else (k (ad-error "Argument to unperturb ~a a perturbation value"
			 v-perturbation)))))
    (else (internal-error))))))

(define (primal v-forward)
 ;; This is written in CPS so as not to break structure sharing.
 (when (and *expensive-checks?* *interned?*)
  (check-canonize-cache! v-forward)
  (check-intern-cache! v-forward)
  (check-interned! v-forward))
 (time-it-bucket
  12
  (let loop ((v-forward v-forward)
	     (top? #t)
	     (k canonize-and-maybe-intern-abstract-value))
   (cond
    ((and (eq? *mode* 'symbolic)
	  (not top?)
	  (not (inline-primal? (abstractify v-forward))))
     (let ((v-forward-abstract (abstractify v-forward)))
      (k (new-call-unit
	  (with-abstract (lambda () (primal v-forward-abstract)))
	  (c:builtin-name "primal" v-forward-abstract)
	  v-forward))))
    ((unit? v-forward)
     (if (vlad-real? (unit-abstract-value v-forward))
	 (k (new-panic-unit "Argument to primal is not a forward value"))
	 (loop (unroll v-forward) top? k)))
    ((union? v-forward)
     (if (union-primal-cache v-forward)
	 (k (union-primal-cache v-forward))
	 (let ((v (create-tagged-union (union-tag v-forward) 'unfilled)))
	  (set-union-primal-cache! v-forward v)
	  (map-cps-non-cs (lambda (v k) (loop v #f k))
			  (union-members v-forward)
			  (lambda (us)
			   (fill-union-values! v us)
			   (k v))))))
    ((vlad-empty-list? v-forward)
     (k (ad-error "Argument to primal ~a a forward value" v-forward)))
    ((vlad-true? v-forward)
     (k (ad-error "Argument to primal ~a a forward value" v-forward)))
    ((vlad-false? v-forward)
     (k (ad-error "Argument to primal ~a a forward value" v-forward)))
    ((vlad-real? v-forward)
     (k (ad-error "Argument to primal ~a a forward value" v-forward)))
    ((primitive-procedure? v-forward)
     (k (ad-error "Argument to primal ~a a forward value" v-forward)))
    ((nonrecursive-closure? v-forward)
     (if (nonrecursive-closure-primal-cache v-forward)
	 (k (nonrecursive-closure-primal-cache v-forward))
	 (let ((b (find-if
		   (lambda (b)
		    (deep-abstract-value=?
		     v-forward
		     (primitive-procedure-forward (value-binding-value b))))
		   *value-bindings*)))
	  (cond
	   (b (let ((u (value-binding-value b)))
	       (set-nonrecursive-closure-primal-cache! v-forward u)
	       (k u)))
	   ((tagged? 'forward (nonrecursive-closure-tags v-forward))
	    ;; See the note in abstract-environment=?.
	    (let ((u (allocate-nonrecursive-closure
		      'unfilled
		      (forward-transform-inverse
		       (nonrecursive-closure-lambda-expression v-forward)))))
	     (set-nonrecursive-closure-primal-cache! v-forward u)
	     (map-cps-non-cs (lambda (v k) (loop v #f k))
			     (get-nonrecursive-closure-values v-forward)
			     (lambda (vs)
			      (fill-nonrecursive-closure-values! u vs)
			      (k u)))))
	   (else (k (ad-error "Argument to primal ~a a forward value"
			      v-forward)))))))
    ((recursive-closure? v-forward)
     (cond ((recursive-closure-primal-cache v-forward)
	    (k (recursive-closure-primal-cache v-forward)))
	   ((tagged? 'forward (recursive-closure-tags v-forward))
	    ;; See the note in abstract-environment=?.
	    (let ((u (allocate-recursive-closure
		      'unfilled
		      (map-vector
		       unforwardify
		       (recursive-closure-procedure-variables v-forward))
		      (map-vector
		       forward-transform-inverse
		       (recursive-closure-lambda-expressions v-forward))
		      (recursive-closure-index v-forward))))
	     (set-recursive-closure-primal-cache! v-forward u)
	     (map-cps-non-cs (lambda (v k) (loop v #f k))
			     (get-recursive-closure-values v-forward)
			     (lambda (vs)
			      (fill-recursive-closure-values! u vs)
			      (k u)))))
	   (else (k (ad-error "Argument to primal ~a a forward value"
			      v-forward)))))
    ((perturbation-tagged-value? v-forward)
     (k (ad-error "Argument to primal ~a a forward value" v-forward)))
    ((bundle? v-forward) (k (get-bundle-primal v-forward)))
    ((sensitivity-tagged-value? v-forward)
     (k (ad-error "Argument to primal ~a a forward value" v-forward)))
    ((reverse-tagged-value? v-forward)
     (k (ad-error "Argument to primal ~a a forward value" v-forward)))
    ((tagged-pair? v-forward)
     (cond ((tagged-pair-primal-cache v-forward)
	    (k (tagged-pair-primal-cache v-forward)))
	   ((tagged? 'forward (tagged-pair-tags v-forward))
	    (let ((u (allocate-tagged-pair
		      (remove-tag 'forward (tagged-pair-tags v-forward))
		      'unfilled
		      'unfilled)))
	     (set-tagged-pair-primal-cache! v-forward u)
	     (loop (get-tagged-pair-car v-forward)
		   #f
		   (lambda (v-car)
		    (loop (get-tagged-pair-cdr v-forward)
			  #f
			  (lambda (v-cdr)
			   (fill-tagged-pair! u v-car v-cdr)
			   (k u)))))))
	   (else (k (ad-error "Argument to primal ~a a forward value"
			      v-forward)))))
    (else (internal-error))))))

(define (tangent v-forward)
 ;; This is written in CPS so as not to break structure sharing.
 (when (and *expensive-checks?* *interned?*)
  (check-canonize-cache! v-forward)
  (check-intern-cache! v-forward)
  (check-interned! v-forward))
 (time-it-bucket
  13
  (let loop ((v-forward v-forward)
	     (top? #t)
	     (k canonize-and-maybe-intern-abstract-value))
   (cond
    ((and (eq? *mode* 'symbolic)
	  (not top?)
	  (not (inline-tangent? (abstractify v-forward))))
     (let ((v-forward-abstract (abstractify v-forward)))
      (k (new-call-unit
	  (with-abstract (lambda () (tangent v-forward-abstract)))
	  (c:builtin-name "tangent" v-forward-abstract)
	  v-forward))))
    ((unit? v-forward)
     (if (vlad-real? (unit-abstract-value v-forward))
	 (k (new-panic-unit "Argument to tangent is not a forward value"))
	 (loop (unroll v-forward) top? k)))
    ((union? v-forward)
     (if (union-tangent-cache v-forward)
	 (k (union-tangent-cache v-forward))
	 (let ((v-perturbation
		(create-tagged-union (union-tag v-forward) 'unfilled)))
	  (set-union-tangent-cache! v-forward v-perturbation)
	  (map-cps-non-cs (lambda (v k) (loop v #f k))
			  (union-members v-forward)
			  (lambda (us-perturbation)
			   (fill-union-values! v-perturbation us-perturbation)
			   (k v-perturbation))))))
    ((vlad-empty-list? v-forward)
     (k (ad-error "Argument to tangent ~a a forward value" v-forward)))
    ((vlad-true? v-forward)
     (k (ad-error "Argument to tangent ~a a forward value" v-forward)))
    ((vlad-false? v-forward)
     (k (ad-error "Argument to tangent ~a a forward value" v-forward)))
    ((vlad-real? v-forward)
     (k (ad-error "Argument to tangent ~a a forward value" v-forward)))
    ((primitive-procedure? v-forward)
     (k (ad-error "Argument to tangent ~a a forward value" v-forward)))
    ((nonrecursive-closure? v-forward)
     (if (nonrecursive-closure-tangent-cache v-forward)
	 (k (nonrecursive-closure-tangent-cache v-forward))
	 (let ((b (find-if
		   (lambda (b)
		    (deep-abstract-value=?
		     v-forward
		     (primitive-procedure-forward (value-binding-value b))))
		   *value-bindings*)))
	  (cond
	   (b (let ((u-perturbation (perturb (value-binding-value b))))
	       (set-nonrecursive-closure-tangent-cache!
		v-forward u-perturbation)
	       (k u-perturbation)))
	   ((tagged? 'forward (nonrecursive-closure-tags v-forward))
	    ;; See the note in abstract-environment=?.
	    (let ((u-perturbation
		   (allocate-nonrecursive-closure
		    'unfilled
		    (perturbation-transform
		     (forward-transform-inverse
		      (nonrecursive-closure-lambda-expression v-forward))))))
	     (set-nonrecursive-closure-tangent-cache! v-forward u-perturbation)
	     (map-cps-non-cs (lambda (v k) (loop v #f k))
			     (get-nonrecursive-closure-values v-forward)
			     (lambda (vs-perturbation)
			      (fill-nonrecursive-closure-values!
			       u-perturbation vs-perturbation)
			      (k u-perturbation)))))
	   (else (k (ad-error "Argument to tangent ~a a forward value"
			      v-forward)))))))
    ((recursive-closure? v-forward)
     (cond ((recursive-closure-tangent-cache v-forward)
	    (k (recursive-closure-tangent-cache v-forward)))
	   ((tagged? 'forward (recursive-closure-tags v-forward))
	    ;; See the note in abstract-environment=?.
	    (let ((u-perturbation
		   (allocate-recursive-closure
		    'unfilled
		    (map-vector
		     (lambda (x) (perturbationify (unforwardify x)))
		     (recursive-closure-procedure-variables v-forward))
		    (map-vector
		     (lambda (e)
		      (perturbation-transform (forward-transform-inverse e)))
		     (recursive-closure-lambda-expressions v-forward))
		    (recursive-closure-index v-forward))))
	     (set-recursive-closure-tangent-cache! v-forward u-perturbation)
	     (map-cps-non-cs (lambda (v k) (loop v #f k))
			     (get-recursive-closure-values v-forward)
			     (lambda (vs-perturbation)
			      (fill-recursive-closure-values!
			       u-perturbation vs-perturbation)
			      (k u-perturbation)))))
	   (else (k (ad-error "Argument to tangent ~a a forward value"
			      v-forward)))))
    ((perturbation-tagged-value? v-forward)
     (k (ad-error "Argument to tangent ~a a forward value" v-forward)))
    ((bundle? v-forward) (k (get-bundle-tangent v-forward)))
    ((sensitivity-tagged-value? v-forward)
     (k (ad-error "Argument to tangent ~a a forward value" v-forward)))
    ((reverse-tagged-value? v-forward)
     (k (ad-error "Argument to tangent ~a a forward value" v-forward)))
    ((tagged-pair? v-forward)
     (cond ((tagged-pair-tangent-cache v-forward)
	    (k (tagged-pair-tangent-cache v-forward)))
	   ((tagged? 'forward (tagged-pair-tags v-forward))
	    (let ((u-perturbation
		   (allocate-tagged-pair
		    (add-tag
		     'perturbation
		     (remove-tag 'forward (tagged-pair-tags v-forward)))
		    'unfilled
		    'unfilled)))
	     (set-tagged-pair-tangent-cache! v-forward u-perturbation)
	     (loop (get-tagged-pair-car v-forward)
		   #f
		   (lambda (v-car-perturbation)
		    (loop (get-tagged-pair-cdr v-forward)
			  #f
			  (lambda (v-cdr-perturbation)
			   (fill-tagged-pair! u-perturbation
					      v-car-perturbation
					      v-cdr-perturbation)
			   (k u-perturbation)))))))
	   (else (k (ad-error "Argument to tangent ~a a forward value"
			      v-forward)))))
    (else (internal-error))))))

(define (bundle v)
 ;; This is written in CPS so as not to break structure sharing.
 (when (and *expensive-checks?* *interned?*)
  (check-canonize-cache! v)
  (check-intern-cache! v)
  (check-interned! v))
 ;; needs work: Throughout the following can mutually narrow v and
 ;;             v-perturbation when creating a bundle to those elements that
 ;;             mututally bundlable with the corresponding elements of the
 ;;             other.
 (time-it-bucket
  14
  ;; needs work: v0 naming convention
  (let loop ((v0 v) (top? #t) (k canonize-and-maybe-intern-abstract-value))
   (cond
    ((and (eq? *mode* 'symbolic)
	  (not top?)
	  (not (inline-bundle? (abstractify v0))))
     (let ((v-abstract (abstractify v0)))
      (k (new-call-unit (with-abstract (lambda () (bundle v-abstract)))
			(c:builtin-name "bundle" v-abstract)
			v))))
    ((unit? v0)
     (if (vlad-real? (unit-abstract-value v0))
	 (k (new-panic-unit "Argument to bundle is not valid"))
	 (loop (unroll v0) top? k)))
    ((union? v0)
     (if (union-bundle-cache v0)
	 (k (union-bundle-cache v0))
	 (let ((v-forward (create-tagged-union (union-tag v0) 'unfilled)))
	  (set-union-bundle-cache! v0 v-forward)
	  (map-cps-non-cs (lambda (v k) (loop v #f k))
			  (union-members v0)
			  (lambda (us-forward)
			   (fill-union-values! v-forward us-forward)
			   (k v-forward))))))
    ((vlad-pair? v0)
     (if (tagged-pair-bundle-cache v0)
	 (k (tagged-pair-bundle-cache v0))
	 (let ((v (vlad-car v0)) (v-perturbation (vlad-cdr v0)))
	  (cond
	   ((and (unit? v) (not (vlad-real? (unit-abstract-value v))))
	    (loop (vlad-cons (unroll v) v-perturbation) top? k))
	   ((and (unit? v-perturbation)
		 (not (vlad-real? (unit-abstract-value v-perturbation))))
	    (loop (vlad-cons v (unroll v-perturbation)) top? k))
	   ((and (perturbation-tagged-value? v-perturbation)
		 (unit? (perturbation-tagged-value-primal v-perturbation))
		 (not (vlad-real?
		       (unit-abstract-value
			(perturbation-tagged-value-primal v-perturbation)))))
	    (loop (vlad-cons
		   v
		   (new-perturbation-tagged-value
		    (unroll
		     (perturbation-tagged-value-primal v-perturbation))))
		  top?
		  k))
	   ;; needs work: The symbolic and abstract values will not correspond.
	   ((and (eq? *mode* 'symbolic)
		 (perturbation-tagged-value? v-perturbation)
		 (union? (perturbation-tagged-value-primal v-perturbation)))
	    (k (create-tagged-union
		(union-tag (perturbation-tagged-value-primal v-perturbation))
		(map (lambda (u)
		      (loop (vlad-cons v (new-perturbation-tagged-value u))
			    #f
			    identity))
		     (get-union-values
		      (perturbation-tagged-value-primal v-perturbation))))))
	   ((and (unit? v)
		 (vlad-real? (unit-abstract-value v))
		 (perturbation-tagged-value? v-perturbation)
		 (unit? (perturbation-tagged-value-primal v-perturbation))
		 (vlad-real?
		  (unit-abstract-value
		   (perturbation-tagged-value-primal v-perturbation))))
	    (k (new-bundle v v-perturbation)))
	   ((union? v)
	    (let ((v-forward (create-tagged-union (union-tag v) 'unfilled)))
	     (set-tagged-pair-bundle-cache! v0 v-forward)
	     (map-cps-non-cs
	      (lambda (u k) (loop (vlad-cons u v-perturbation) #f k))
	      (union-members v)
	      (lambda (us-forward)
	       (fill-union-values! v-forward us-forward)
	       (k v-forward)))))
	   ((union? v-perturbation)
	    (let ((v-forward
		   (create-tagged-union (union-tag v-perturbation) 'unfilled)))
	     (set-tagged-pair-bundle-cache! v0 v-forward)
	     (map-cps-non-cs (lambda (u-perturbation k)
			      (loop (vlad-cons v u-perturbation) #f k))
			     (union-members v-perturbation)
			     (lambda (us-forward)
			      (fill-union-values! v-forward us-forward)
			      (k v-forward)))))
	   ((and (vlad-empty-list? v) (some-bundlable? v v-perturbation))
	    (unless (every-bundlable? v v-perturbation)
	     (ad-warning
	      "Arguments to bundle might not conform" v v-perturbation))
	    (let ((u-forward (new-bundle v v-perturbation)))
	     (set-tagged-pair-bundle-cache! v0 u-forward)
	     (k u-forward)))
	   ((and (vlad-true? v) (some-bundlable? v v-perturbation))
	    (unless (every-bundlable? v v-perturbation)
	     (ad-warning
	      "Arguments to bundle might not conform" v v-perturbation))
	    (let ((u-forward (new-bundle v v-perturbation)))
	     (set-tagged-pair-bundle-cache! v0 u-forward)
	     (k u-forward)))
	   ((and (vlad-false? v) (some-bundlable? v v-perturbation))
	    (unless (every-bundlable? v v-perturbation)
	     (ad-warning
	      "Arguments to bundle might not conform" v v-perturbation))
	    (let ((u-forward (new-bundle v v-perturbation)))
	     (set-tagged-pair-bundle-cache! v0 u-forward)
	     (k u-forward)))
	   ((and (vlad-real? v) (some-bundlable? v v-perturbation))
	    (unless (every-bundlable? v v-perturbation)
	     (ad-warning
	      "Arguments to bundle might not conform" v v-perturbation))
	    (let ((u-forward (new-bundle v v-perturbation)))
	     (set-tagged-pair-bundle-cache! v0 u-forward)
	     (k u-forward)))
	   ((primitive-procedure? v)
	    (unless (every-bundlable? v v-perturbation)
	     (ad-warning
	      "Arguments to bundle might not conform" v v-perturbation))
	    (let ((u-forward (primitive-procedure-forward v)))
	     (set-tagged-pair-bundle-cache! v0 u-forward)
	     (k u-forward)))
	   ((and (nonrecursive-closure? v)
		 (nonrecursive-closure? v-perturbation)
		 (perturbation-parameter?
		  (nonrecursive-closure-parameter v-perturbation))
		 (dereferenced-nonrecursive-closure-match?
		  v (unperturb v-perturbation)))
	    ;; See the note in abstract-environment=?.
	    (let ((u-forward (allocate-nonrecursive-closure
			      'unfilled
			      (forward-transform
			       (nonrecursive-closure-lambda-expression v)))))
	     (set-nonrecursive-closure-primal-cache! u-forward v)
	     (set-nonrecursive-closure-tangent-cache! u-forward v-perturbation)
	     (set-tagged-pair-bundle-cache! v0 u-forward)
	     (map2-cps-non-cs
	      (lambda (v v-perturbation k)
	       (loop (vlad-cons v v-perturbation) #f k))
	      (get-nonrecursive-closure-values v)
	      (get-nonrecursive-closure-values v-perturbation)
	      (lambda (vs-forward)
	       (fill-nonrecursive-closure-values! u-forward vs-forward)
	       (k u-forward)))))
	   ((and (recursive-closure? v)
		 (recursive-closure? v-perturbation)
		 (perturbation-parameter?
		  (recursive-closure-parameter v-perturbation))
		 (dereferenced-recursive-closure-match?
		  v (unperturb v-perturbation)))
	    ;; See the note in abstract-environment=?.
	    (let ((u-forward
		   (allocate-recursive-closure
		    'unfilled
		    (map-vector forwardify
				(recursive-closure-procedure-variables v))
		    (map-vector forward-transform
				(recursive-closure-lambda-expressions v))
		    (recursive-closure-index v))))
	     (set-recursive-closure-primal-cache! u-forward v)
	     (set-recursive-closure-tangent-cache! u-forward v-perturbation)
	     (set-tagged-pair-bundle-cache! v0 u-forward)
	     (map2-cps-non-cs
	      (lambda (v v-perturbation k)
	       (loop (vlad-cons v v-perturbation) #f k))
	      (get-recursive-closure-values v)
	      (get-recursive-closure-values v-perturbation)
	      (lambda (vs-forward)
	       (fill-recursive-closure-values! u-forward vs-forward)
	       (k u-forward)))))
	   ((and (perturbation-tagged-value? v)
		 (some-bundlable? v v-perturbation))
	    (unless (every-bundlable? v v-perturbation)
	     (ad-warning
	      "Arguments to bundle might not conform" v v-perturbation))
	    (let ((u-forward (create-bundle v v-perturbation)))
	     (set-tagged-pair-bundle-cache! v0 u-forward)
	     (k u-forward)))
	   ((and (bundle? v) (some-bundlable? v v-perturbation))
	    (unless (every-bundlable? v v-perturbation)
	     (ad-warning
	      "Arguments to bundle might not conform" v v-perturbation))
	    (let ((u-forward (create-bundle v v-perturbation)))
	     (set-tagged-pair-bundle-cache! v0 u-forward)
	     (k u-forward)))
	   ((and (sensitivity-tagged-value? v)
		 (some-bundlable? v v-perturbation))
	    (unless (every-bundlable? v v-perturbation)
	     (ad-warning
	      "Arguments to bundle might not conform" v v-perturbation))
	    (let ((u-forward (create-bundle v v-perturbation)))
	     (set-tagged-pair-bundle-cache! v0 u-forward)
	     (k u-forward)))
	   ((and (reverse-tagged-value? v) (some-bundlable? v v-perturbation))
	    (unless (every-bundlable? v v-perturbation)
	     (ad-warning
	      "Arguments to bundle might not conform" v v-perturbation))
	    (let ((u-forward (create-bundle v v-perturbation)))
	     (set-tagged-pair-bundle-cache! v0 u-forward)
	     (k u-forward)))
	   ((and
	     (tagged-pair? v)
	     (tagged-pair? v-perturbation)
	     (tagged? 'perturbation (tagged-pair-tags v-perturbation))
	     (equal-tags?
	      (tagged-pair-tags v)
	      (remove-tag 'perturbation (tagged-pair-tags v-perturbation))))
	    (let ((u-forward (allocate-tagged-pair
			      (add-tag 'forward (tagged-pair-tags v))
			      'unfilled
			      'unfilled)))
	     (set-tagged-pair-primal-cache! u-forward v)
	     (set-tagged-pair-tangent-cache! u-forward v-perturbation)
	     (set-tagged-pair-bundle-cache! v0 u-forward)
	     (loop
	      (vlad-cons (get-tagged-pair-car v)
			 (get-tagged-pair-car v-perturbation))
	      #f
	      (lambda (v-car-forward)
	       (loop (vlad-cons (get-tagged-pair-cdr v)
				(get-tagged-pair-cdr v-perturbation))
		     #f
		     (lambda (v-cdr-forward)
		      (fill-tagged-pair! u-forward v-car-forward v-cdr-forward)
		      (k u-forward)))))))
	   (else (case *mode*
		  ((concrete)
		   (run-time-error "Arguments to bundle do not conform"
				   v
				   v-perturbation))
		  ((abstract)
		   (let ((u-forward (compile-time-warning
				     "Arguments to bundle might not conform"
				     v
				     v-perturbation)))
		    (set-tagged-pair-bundle-cache! v0 u-forward)
		    (k u-forward)))
		  ((symbolic)
		   (k (new-panic-unit "Arguments to bundle do not conform")))
		  (else (internal-error))))))))
    (else (k (ad-error "Argument to bundle ~a valid" v0)))))))

(define (j* v) (bundle (vlad-cons v (zero (perturb v)))))

;;; Reverse Mode

(define (added-variable x)
 (new-constant-expression
  (value-binding-value
   (find-if
    (lambda (b)
     (concrete-variable=? x (variable-name (value-binding-variable b))))
    *value-bindings*))))

(define (make-sensitize e) (new-application (added-variable 'sensitize) e))

(define (make-zero e) (new-application (added-variable 'zero) e))

(define (make-plus e1 e2)
 (new-application (added-variable 'plus) (create-cons-expression e1 e2)))

(define (make-plus-binding p e) (make-parameter-binding p (make-plus p e)))

(define (make-*j-inverse e) (new-application (added-variable '*j-inverse) e))

;;; We no longer check for unsupported letrec-expression parameter.
(define (sensitivityify-parameter p) (sensitivity-transform p))

(define (reverseify-parameter p)
 (cond
  ((constant-expression? p)
   (with-concrete
    (lambda () (new-constant-expression (*j (constant-expression-value p))))))
  ((variable-access-expression? p) (reverseify-access p))
  ((lambda-expression? p) (reverse-transform p))
  ((letrec-expression? p)
   (assert (and (variable-access-expression? (letrec-expression-body p))
		(memp variable=?
		      (variable-access-expression-variable
		       (letrec-expression-body p))
		      (letrec-expression-procedure-variables p))))
   (new-letrec-expression
    (map reverseify (letrec-expression-procedure-variables p))
    (map-indexed (lambda (e i)
		  (reverse-transform-internal
		   e
		   (letrec-expression-procedure-variables p)
		   (letrec-expression-lambda-expressions p)
		   i))
		 (letrec-expression-lambda-expressions p))
    (reverseify-access (letrec-expression-body p))))
  ((cons-expression? p)
   (new-cons-expression (add-tag 'reverse (cons-expression-tags p))
			(reverseify-parameter (cons-expression-car p))
			(reverseify-parameter (cons-expression-cdr p))))
  (else (internal-error))))

(define (sensitivity-transform e)
 (if (and (lambda-expression? e) (lambda-expression-sensitivity-transform e))
     (lambda-expression-sensitivity-transform e)
     (let ((e1 (cond
		((constant-expression? e)
		 (with-concrete
		  (lambda ()
		   (new-constant-expression
		    (sensitize (constant-expression-value e))))))
		((variable-access-expression? e) (sensitivityify-access e))
		((lambda-expression? e)
		 (new-lambda-expression
		  (sensitivity-transform (lambda-expression-parameter e))
		  (sensitivity-transform (lambda-expression-body e))))
		((application? e)
		 (new-application
		  (sensitivity-transform (application-callee e))
		  (sensitivity-transform (application-argument e))))
		((letrec-expression? e)
		 (new-letrec-expression
		  (map sensitivityify
		       (letrec-expression-procedure-variables e))
		  (map sensitivity-transform
		       (letrec-expression-lambda-expressions e))
		  (sensitivity-transform (letrec-expression-body e))))
		((cons-expression? e)
		 (new-cons-expression
		  (add-tag 'sensitivity (cons-expression-tags e))
		  (sensitivity-transform (cons-expression-car e))
		  (sensitivity-transform (cons-expression-cdr e))))
		(else (internal-error)))))
      (when (lambda-expression? e)
       (assert
	(and (not (lambda-expression-sensitivity-transform e))
	     (not (lambda-expression-sensitivity-transform-inverse e1))))
       (set-lambda-expression-sensitivity-transform! e e1)
       (set-lambda-expression-sensitivity-transform-inverse! e1 e))
      e1)))

(define (sensitivity-transform-inverse? e)
 (assert (lambda-expression? e))
 (not (not (lambda-expression-sensitivity-transform-inverse e))))

(define (sensitivity-transform-inverse e)
 (assert (and (lambda-expression? e)
	      (lambda-expression-sensitivity-transform-inverse e)))
 (lambda-expression-sensitivity-transform-inverse e))

(define (reverse-transform-internal e xs0 es0 i)
 ;; e  is a lambda expression. Its body is in anf. Its body is a letrec
 ;;    expression, unless it has been optimized away.
 ;; xs1 is the procedure variables of the body of e, when it is a letrec
 ;;     expression. Otherwise it is empty.
 ;; es1 is the lambda expressions of the body of e, when it is a letrec
 ;;     expression. Otherwise it is empty.
 ;; xs0 is the procedure variables of the surrounding letrec or recursive
 ;;     closure when e is a letrec expression lambda expression or a recursive
 ;;     closure lambda expression. Otherwise it is empty.
 ;; es0 is the lambda expressions of the surrounding letrec or recursive
 ;;     closure when e is a letrec expression lambda expression or a recursive
 ;;     closure lambda expression. Otherwise it is empty.
 (assert (lambda-expression? e))
 (if (lambda-expression-reverse-transform e)
     (lambda-expression-reverse-transform e)
     (let* ((p (lambda-expression-parameter e))
	    (e1 (lambda-expression-body e))
	    (xs1 (if (letrec-expression? e1)
		     (letrec-expression-procedure-variables e1)
		     '()))
	    (es1 (if (letrec-expression? e1)
		     (letrec-expression-lambda-expressions e1)
		     '()))
	    ;; I am not 100% sure that this cannot cause name clash. One way to
	    ;; guarantee that there is no name clash is to find the highest
	    ;; index of a backpropagator variable in e1 and generate new
	    ;; indices larger than that.
	    (xs (map-n backpropagatorify (length (anf-let*-parameters e1))))
	    (e2
	     ;; The only portion of this that needs to be anf converted is the
	     ;; cons expression in the body of the let* that returns the primal
	     ;; paired with the backpropagator (except for the backpropagator
	     ;; which is independently alpha/anf converted).
	     (anf-convert-lambda-expression-shallow
	      ;; This doesn't need to be alpha converted since it is derived
	      ;; straightforwardly from an expression that is already alpha
	      ;; converted.
	      (new-lambda-expression
	       (reverseify-parameter p)
	       (new-letrec-expression
		(map reverseify xs1)
		(if (letrec-expression? e1)
		    (map-indexed
		     (lambda (e i) (reverse-transform-internal e xs1 es1 i))
		     es1)
		    '())
		(create-let*
		 ;; These are the bindings for the forward phase that come from
		 ;; the primal.
		 (map
		  (lambda (p e x)
		   (cond
		    ;;            /   /
		    ;;            _   _
		    ;; p = v -~-> p = v
		    ((constant-expression? e)
		     (make-parameter-binding
		      (reverseify-parameter p)
		      (with-concrete
		       (lambda ()
			(new-constant-expression
			 (*j (constant-expression-value e)))))))
		    ;;            /   /
		    ;;            _   _
		    ;; p = e -~-> p = e
		    ((variable-access-expression? e)
		     (make-parameter-binding
		      (reverseify-parameter p) (reverseify-access e)))
		    ;;                /   /
		    ;;                _   ______
		    ;; p = \ x e -~-> p = \ x e
		    ((lambda-expression? e)
		     (make-parameter-binding
		      (reverseify-parameter p) (reverse-transform e)))
		    ;;                /     /  /
		    ;;                _ _   __ __
		    ;; p = x1 x2 -~-> p,p = x1 x2
		    ((application? e)
		     (make-parameter-binding
		      (create-cons-expression
		       (reverseify-parameter p)
		       (new-variable-access-expression x))
		      (new-application
		       (reverseify-access (application-callee e))
		       (reverseify-access (application-argument e)))))
		    ;;                /   /  / /
		    ;;                _   __ _ __
		    ;; p = x1,x2 -~-> p = x1 , x2
		    ((cons-expression? e)
		     (make-parameter-binding
		      (reverseify-parameter p)
		      (new-cons-expression
		       (add-tag 'reverse (cons-expression-tags e))
		       (reverseify-access (cons-expression-car e))
		       (reverseify-access (cons-expression-cdr e)))))
		    (else (internal-error))))
		  (anf-let*-parameters e1)
		  (anf-let*-expressions e1)
		  xs)
		 ;; This conses the result of the forward phase with the
		 ;; backpropagator.
		 (create-cons-expression
		  ;; This is the result of the forward phase.
		  (reverseify-parameter (anf-parameter e1))
		  ;; This is the backpropagator.
		  (anf-convert-lambda-expression-for-reverse
		   (alpha-convert
		    (new-lambda-expression
		     (sensitivityify-access (anf-parameter e1))
		     (create-let*
		      (append
		       ;; These are the zeroing bindings for the reverse phase.
		       (map
			(lambda (x)
			 (make-parameter-binding
			  (sensitivity-access x)
			  (make-zero
			   (make-sensitize
			    (make-*j-inverse (reverse-access x))))))
			(set-differencep
			 variable=?
			 (remove-duplicatesp
			  variable=?
			  (append
			   (parameter-variables p)
			   (map-reduce append
				       '()
				       parameter-variables
				       (anf-let*-parameters e1))
			   xs1
			   ;; needs work: Why is
			   ;;             (recursive-closure-free-variables
			   ;;              xs1 es1)
			   ;;             not here?
			   xs0
			   (if (= i -1)
			       (free-variables e)
			       (recursive-closure-free-variables xs0 es0))))
			 (parameter-variables (anf-parameter e1))))
		       ;; These are the bindings for the reverse phase that
		       ;; come from the primal.
		       (removeq
			#f
			(map
			 (lambda (p e x)
			  (cond
			   ;; p = v is eliminated
			   ((constant-expression? e) #f)
			   ;;            _    _
			   ;;            \    \
			   ;; p = e -~-> e += p
			   ((variable-access-expression? e)
			    (make-plus-binding (sensitivityify-access e)
					       (sensitivityify-parameter p)))
			   ;;                _____    _
			   ;;                \        \
			   ;; p = \ x e -~-> \ x e += p
			   ((lambda-expression? e)
			    (make-plus-binding (sensitivity-transform e)
					       (sensitivityify-parameter p)))
			   ;;                __ _ __    _ _
			   ;;                \  \ \       \
			   ;; p = x1 x2 -~-> x1 , x2 += p p
			   ;; We want the x1,x2 inside the sensitivity so that
			   ;; the aggregate is a sensitivity that can be added
			   ;; by plus, since for type correctness, plus adds
			   ;; only sensitivities.
			   ((application? e)
			    (make-plus-binding
			     (new-cons-expression
			      (add-tag 'sensitivity (empty-tags))
			      (sensitivityify-access (application-callee e))
			      (sensitivityify-access (application-argument e)))
			     (new-application
			      (new-variable-access-expression x)
			      (sensitivityify-parameter p))))
			   ;;                __ _ __    _
			   ;;                \  \ \     \
			   ;; p = x1,x2 -~-> x1 , x2 += p
			   ;; We want the x1,x2 inside the sensitivity so that
			   ;; the aggregate is a sensitivity that can be added
			   ;; by plus, since for type correctness, plus adds
			   ;; only sensitivities.
			   ((cons-expression? e)
			    (make-plus-binding
			     (new-cons-expression
			      (add-tag 'sensitivity (cons-expression-tags e))
			      (sensitivityify-access (cons-expression-car e))
			      (sensitivityify-access (cons-expression-cdr e)))
			     (sensitivityify-parameter p)))
			   (else (internal-error))))
			 (reverse (anf-let*-parameters e1))
			 (reverse (anf-let*-expressions e1))
			 (reverse xs)))
		       (map (lambda (x1)
			     ;; ______________________    __
			     ;; \                         \
			     ;; letrec xs1 = es1 in x1 += x1
			     (make-plus-binding
			      (sensitivity-transform
			       (new-letrec-expression
				xs1 es1 (new-variable-access-expression x1)))
			      (sensitivity-access x1)))
			    xs1)
		       (map (lambda (x0)
			     ;; ______________________    __
			     ;; \                         \
			     ;; letrec xs0 = es0 in x0 += x0
			     (make-plus-binding
			      (sensitivity-transform
			       (new-letrec-expression
				xs0 es0 (new-variable-access-expression x0)))
			      (sensitivity-access x0)))
			    xs0))
		      ;; This conses the sensitivity to the target with the
		      ;; sensitivity to the argument.
		      (new-cons-expression
		       (add-tag 'sensitivity (empty-tags))
		       ;; This is the sensitivity to the target.
		       (sensitivity-transform
			(if (= i -1)
			    ;; _
			    ;; \
			    ;; e
			    e
			    ;; ______________________
			    ;; \
			    ;; letrec xs0 = es0 in x0
			    (new-letrec-expression
			     xs0
			     es0
			     (new-variable-access-expression
			      (list-ref xs0 i)))))
		       ;; This is the sensitivity to the argument.
		       (sensitivityify-parameter p)))))))))))))
      (assert (and (not (lambda-expression-reverse-transform e))
		   (not (lambda-expression-reverse-transform-inverse e2))))
      (set-lambda-expression-reverse-transform! e e2)
      (set-lambda-expression-reverse-transform-inverse! e2 e)
      e2)))

(define (reverse-transform e) (reverse-transform-internal e '() '() -1))

(define (reverse-transform-inverse e)
 (assert (and (lambda-expression? e)
	      (lambda-expression-reverse-transform-inverse e)))
 (lambda-expression-reverse-transform-inverse e))

(define (sensitize v)
 ;; This is written in CPS so as not to break structure sharing.
 (when (and *expensive-checks?* *interned?*)
  (check-canonize-cache! v)
  (check-intern-cache! v)
  (check-interned! v))
 (time-it-bucket
  15
  (let loop ((v v) (top? #t) (k canonize-and-maybe-intern-abstract-value))
   (cond
    ((and (eq? *mode* 'symbolic)
	  (not top?)
	  (not (inline-sensitize? (abstractify v))))
     (let ((v-abstract (abstractify v)))
      (k (new-call-unit (with-abstract (lambda () (sensitize v-abstract)))
			(c:builtin-name "sensitize" v-abstract)
			v))))
    ((unit? v)
     (if (vlad-real? (unit-abstract-value v))
	 (k (new-sensitivity-tagged-value v))
	 (loop (unroll v) top? k)))
    ((union? v)
     (if (union-sensitize-cache v)
	 (k (union-sensitize-cache v))
	 (let ((v-sensitivity (create-tagged-union (union-tag v) 'unfilled)))
	  (set-union-unsensitize-cache! v-sensitivity v)
	  (set-union-sensitize-cache! v v-sensitivity)
	  (map-cps-non-cs (lambda (v k) (loop v #f k))
			  (union-members v)
			  (lambda (us-sensitivity)
			   (fill-union-values! v-sensitivity us-sensitivity)
			   (k v-sensitivity))))))
    ((vlad-empty-list? v) (k (new-sensitivity-tagged-value v)))
    ((vlad-true? v) (k (new-sensitivity-tagged-value v)))
    ((vlad-false? v) (k (new-sensitivity-tagged-value v)))
    ((vlad-real? v) (k (new-sensitivity-tagged-value v)))
    ((primitive-procedure? v) (k (new-sensitivity-tagged-value v)))
    ((nonrecursive-closure? v)
     (if (nonrecursive-closure-sensitize-cache v)
	 (k (nonrecursive-closure-sensitize-cache v))
	 ;; See the note in abstract-environment=?.
	 (let ((u-sensitivity (allocate-nonrecursive-closure
			       'unfilled
			       (sensitivity-transform
				(nonrecursive-closure-lambda-expression v)))))
	  (set-nonrecursive-closure-unsensitize-cache! u-sensitivity v)
	  (set-nonrecursive-closure-sensitize-cache! v u-sensitivity)
	  (map-cps-non-cs
	   (lambda (v k) (loop v #f k))
	   (get-nonrecursive-closure-values v)
	   (lambda (vs-sensitivity)
	    (fill-nonrecursive-closure-values! u-sensitivity vs-sensitivity)
	    (k u-sensitivity))))))
    ((recursive-closure? v)
     (if (recursive-closure-sensitize-cache v)
	 (k (recursive-closure-sensitize-cache v))
	 ;; See the note in abstract-environment=?.
	 (let ((u-sensitivity
		(allocate-recursive-closure
		 'unfilled
		 (map-vector sensitivityify
			     (recursive-closure-procedure-variables v))
		 (map-vector sensitivity-transform
			     (recursive-closure-lambda-expressions v))
		 (recursive-closure-index v))))
	  (set-recursive-closure-unsensitize-cache! u-sensitivity v)
	  (set-recursive-closure-sensitize-cache! v u-sensitivity)
	  (map-cps-non-cs
	   (lambda (v k) (loop v #f k))
	   (get-recursive-closure-values v)
	   (lambda (vs-sensitivity)
	    (fill-recursive-closure-values! u-sensitivity vs-sensitivity)
	    (k u-sensitivity))))))
    ((perturbation-tagged-value? v)
     (if (perturbation-tagged-value-sensitize-cache v)
	 (k (perturbation-tagged-value-sensitize-cache v))
	 (let ((u-sensitivity (create-sensitivity-tagged-value v)))
	  (set-perturbation-tagged-value-sensitize-cache! v u-sensitivity)
	  (k u-sensitivity))))
    ((bundle? v)
     (if (bundle-sensitize-cache v)
	 (k (bundle-sensitize-cache v))
	 (let ((u-sensitivity (create-sensitivity-tagged-value v)))
	  (set-bundle-sensitize-cache! v u-sensitivity)
	  (k u-sensitivity))))
    ((sensitivity-tagged-value? v)
     (if (sensitivity-tagged-value-sensitize-cache v)
	 (k (sensitivity-tagged-value-sensitize-cache v))
	 (let ((u-sensitivity (create-sensitivity-tagged-value v)))
	  (set-sensitivity-tagged-value-sensitize-cache! v u-sensitivity)
	  (k u-sensitivity))))
    ((reverse-tagged-value? v)
     (if (reverse-tagged-value-sensitize-cache v)
	 (k (reverse-tagged-value-sensitize-cache v))
	 (let ((u-sensitivity (create-sensitivity-tagged-value v)))
	  (set-reverse-tagged-value-sensitize-cache! v u-sensitivity)
	  (k u-sensitivity))))
    ((tagged-pair? v)
     (if (tagged-pair-sensitize-cache v)
	 (k (tagged-pair-sensitize-cache v))
	 (let ((u-sensitivity (allocate-tagged-pair
			       (add-tag 'sensitivity (tagged-pair-tags v))
			       'unfilled
			       'unfilled)))
	  (set-tagged-pair-unsensitize-cache! u-sensitivity v)
	  (set-tagged-pair-sensitize-cache! v u-sensitivity)
	  (loop (get-tagged-pair-car v)
		#f
		(lambda (v-car-sensitivity)
		 (loop (get-tagged-pair-cdr v)
		       #f
		       (lambda (v-cdr-sensitivity)
			(fill-tagged-pair!
			 u-sensitivity v-car-sensitivity v-cdr-sensitivity)
			(k u-sensitivity))))))))
    (else (internal-error))))))

(define (unsensitize? v-sensitivity)
 ;; This is written in CPS so as not to break structure sharing.
 ;; Unlike the other AD primitives, v-sensitivity might not be canonized or
 ;; interned because canonize-abstract-values calls
 ;; abstract-value-union-internal which calls backpropagator? which calls
 ;; unsensitize?.
 (time-it-bucket
  16
  (let loop ((v-sensitivity v-sensitivity) (cs '()) (k (lambda (r? cs) r?)))
   (let ((found? (memq v-sensitivity cs)))
    (cond
     (found? (k #t cs))
     ((union? v-sensitivity)
      (if (union-unsensitize-cache v-sensitivity)
	  (k #t cs)
	  (every-cps
	   loop (union-members v-sensitivity) (cons v-sensitivity cs) k)))
     ((vlad-empty-list? v-sensitivity) (k #f cs))
     ((vlad-true? v-sensitivity) (k #f cs))
     ((vlad-false? v-sensitivity) (k #f cs))
     ((vlad-real? v-sensitivity) (k #f cs))
     ((primitive-procedure? v-sensitivity) (k #f cs))
     ((nonrecursive-closure? v-sensitivity)
      (cond
       ((nonrecursive-closure-unsensitize-cache v-sensitivity) (k #t cs))
       ((and (tagged? 'sensitivity (nonrecursive-closure-tags v-sensitivity))
	     (sensitivity-transform-inverse?
	      (nonrecursive-closure-lambda-expression v-sensitivity)))
	;; See the note in abstract-environment=?.
	(every-cps loop
		   (get-nonrecursive-closure-values v-sensitivity)
		   (cons v-sensitivity cs)
		   k))
       (else (k #f cs))))
     ((recursive-closure? v-sensitivity)
      (cond
       ((recursive-closure-unsensitize-cache v-sensitivity) (k #t cs))
       ((and
	 (tagged? 'sensitivity (recursive-closure-tags v-sensitivity))
	 (every-vector unsensitivityify?
		       (recursive-closure-procedure-variables v-sensitivity))
	 (every-vector sensitivity-transform-inverse?
		       (recursive-closure-lambda-expressions v-sensitivity)))
	;; See the note in abstract-environment=?.
	(every-cps loop
		   (get-recursive-closure-values v-sensitivity)
		   (cons v-sensitivity cs)
		   k))
       (else (k #f cs))))
     ((perturbation-tagged-value? v-sensitivity) (k #f cs))
     ((bundle? v-sensitivity) (k #f cs))
     ((sensitivity-tagged-value? v-sensitivity) (k #t (cons v-sensitivity cs)))
     ((reverse-tagged-value? v-sensitivity) (k #f cs))
     ((tagged-pair? v-sensitivity)
      (cond ((tagged-pair-unsensitize-cache v-sensitivity) (k #t cs))
	    ((tagged? 'sensitivity (tagged-pair-tags v-sensitivity))
	     (loop (get-tagged-pair-car v-sensitivity)
		   (cons v-sensitivity cs)
		   (lambda (r? cs)
		    (if r?
			(loop (get-tagged-pair-cdr v-sensitivity) cs k)
			(k #f cs)))))
	    (else (k #f cs))))
     (else (internal-error)))))))

(define (unsensitize v-sensitivity)
 ;; This is written in CPS so as not to break structure sharing.
 (when (and *expensive-checks?* *interned?*)
  (check-canonize-cache! v-sensitivity)
  (check-intern-cache! v-sensitivity)
  (check-interned! v-sensitivity))
 (time-it-bucket
  17
  (let loop ((v-sensitivity v-sensitivity)
	     (top? #t)
	     (k canonize-and-maybe-intern-abstract-value))
   (cond
    ((and (eq? *mode* 'symbolic)
	  (not top?)
	  (not (inline-unsensitize? (abstractify v-sensitivity))))
     (let ((v-sensitivity-abstract (abstractify v-sensitivity)))
      (k (new-call-unit
	  (with-abstract (lambda () (unsensitize v-sensitivity-abstract)))
	  (c:builtin-name "unsensitize" v-sensitivity-abstract)
	  v-sensitivity))))
    ((unit? v-sensitivity)
     (if (vlad-real? (unit-abstract-value v-sensitivity))
	 (k (new-panic-unit
	     "Argument to unperturb is not a sensitivity value"))
	 (loop (unroll v-sensitivity) top? k)))
    ((union? v-sensitivity)
     (if (union-unsensitize-cache v-sensitivity)
	 (k (union-unsensitize-cache v-sensitivity))
	 (let ((v (create-tagged-union (union-tag v-sensitivity) 'unfilled)))
	  (set-union-sensitize-cache! v v-sensitivity)
	  (set-union-unsensitize-cache! v-sensitivity v)
	  (map-cps-non-cs (lambda (v k) (loop v #f k))
			  (union-members v-sensitivity)
			  (lambda (us)
			   (fill-union-values! v us)
			   (k v))))))
    ((vlad-empty-list? v-sensitivity)
     (k (ad-error "Argument to unsensitize ~a a sensitivity value"
		  v-sensitivity)))
    ((vlad-true? v-sensitivity)
     (k (ad-error "Argument to unsensitize ~a a sensitivity value"
		  v-sensitivity)))
    ((vlad-false? v-sensitivity)
     (k (ad-error "Argument to unsensitize ~a a sensitivity value"
		  v-sensitivity)))
    ((vlad-real? v-sensitivity)
     (k (ad-error "Argument to unsensitize ~a a sensitivity value"
		  v-sensitivity)))
    ((primitive-procedure? v-sensitivity)
     (k (ad-error "Argument to unsensitize ~a a sensitivity value"
		  v-sensitivity)))
    ((nonrecursive-closure? v-sensitivity)
     (cond
      ((nonrecursive-closure-unsensitize-cache v-sensitivity)
       (k (nonrecursive-closure-unsensitize-cache v-sensitivity)))
      ((tagged? 'sensitivity (nonrecursive-closure-tags v-sensitivity))
       ;; See the note in abstract-environment=?.
       (let ((u (allocate-nonrecursive-closure
		 'unfilled
		 (sensitivity-transform-inverse
		  (nonrecursive-closure-lambda-expression v-sensitivity)))))
	(set-nonrecursive-closure-sensitize-cache! u v-sensitivity)
	(set-nonrecursive-closure-unsensitize-cache! v-sensitivity u)
	(map-cps-non-cs (lambda (v k) (loop v #f k))
			(get-nonrecursive-closure-values v-sensitivity)
			(lambda (vs)
			 (fill-nonrecursive-closure-values! u vs)
			 (k u)))))
      (else (k (ad-error "Argument to unsensitize ~a a sensitivity value"
			 v-sensitivity)))))
    ((recursive-closure? v-sensitivity)
     (cond ((recursive-closure-unsensitize-cache v-sensitivity)
	    (k (recursive-closure-unsensitize-cache v-sensitivity)))
	   ((tagged? 'sensitivity (recursive-closure-tags v-sensitivity))
	    ;; See the note in abstract-environment=?.
	    (let ((u (allocate-recursive-closure
		      'unfilled
		      (map-vector
		       unsensitivityify
		       (recursive-closure-procedure-variables v-sensitivity))
		      (map-vector
		       sensitivity-transform-inverse
		       (recursive-closure-lambda-expressions v-sensitivity))
		      (recursive-closure-index v-sensitivity))))
	     (set-recursive-closure-sensitize-cache! u v-sensitivity)
	     (set-recursive-closure-unsensitize-cache! v-sensitivity u)
	     (map-cps-non-cs (lambda (v k) (loop v #f k))
			     (get-recursive-closure-values v-sensitivity)
			     (lambda (vs)
			      (fill-recursive-closure-values! u vs)
			      (k u)))))
	   (else (k (ad-error "Argument to unsensitize ~a a sensitivity value"
			      v-sensitivity)))))
    ((perturbation-tagged-value? v-sensitivity)
     (k (ad-error "Argument to unsensitize ~a a sensitivity value"
		  v-sensitivity)))
    ((bundle? v-sensitivity)
     (k (ad-error "Argument to unsensitize ~a a sensitivity value"
		  v-sensitivity)))
    ((sensitivity-tagged-value? v-sensitivity)
     (k (get-sensitivity-tagged-value-primal v-sensitivity)))
    ((reverse-tagged-value? v-sensitivity)
     (k (ad-error "Argument to unsensitize ~a a sensitivity value"
		  v-sensitivity)))
    ((tagged-pair? v-sensitivity)
     (cond
      ((tagged-pair-unsensitize-cache v-sensitivity)
       (k (tagged-pair-unsensitize-cache v-sensitivity)))
      ((tagged? 'sensitivity (tagged-pair-tags v-sensitivity))
       (let ((u (allocate-tagged-pair
		 (remove-tag 'sensitivity (tagged-pair-tags v-sensitivity))
		 'unfilled
		 'unfilled)))
	(set-tagged-pair-sensitize-cache! u v-sensitivity)
	(set-tagged-pair-unsensitize-cache! v-sensitivity u)
	(loop (get-tagged-pair-car v-sensitivity)
	      #f
	      (lambda (v-car)
	       (loop (get-tagged-pair-cdr v-sensitivity)
		     #f
		     (lambda (v-cdr)
		      (fill-tagged-pair! u v-car v-cdr)
		      (k u)))))))
      (else (k (ad-error "Argument to unsensitize ~a a sensitivity value"
			 v-sensitivity)))))
    (else (internal-error))))))

(define (is-zero? v)
 ;; A false return value doesn't mean that v is nonzero, it just means that it
 ;; wasn't produced by the zero procedure. It might just happen to be a zero.
 (or
  (and (union? v) (eq? (union-zero-cache v) v))
  (vlad-empty-list? v)
  (vlad-true? v)
  (vlad-false? v)
  (and (real? v) (zero? v))
  (primitive-procedure? v)
  (and (nonrecursive-closure? v) (eq? (nonrecursive-closure-zero-cache v) v))
  (and (recursive-closure? v) (eq? (recursive-closure-zero-cache v) v))
  (and (perturbation-tagged-value? v)
       (eq? (perturbation-tagged-value-zero-cache v) v))
  (and (bundle? v) (eq? (bundle-zero-cache v) v))
  (and (sensitivity-tagged-value? v)
       (eq? (sensitivity-tagged-value-zero-cache v) v))
  (and (reverse-tagged-value? v) (eq? (reverse-tagged-value-zero-cache v) v))
  (and (tagged-pair? v) (eq? (tagged-pair-zero-cache v) v))))

(define (plus v)
 ;; This is written in CPS so as not to break structure sharing.
 (when (and *expensive-checks?* *interned?*)
  (check-canonize-cache! v)
  (check-intern-cache! v)
  (check-interned! v))
 (time-it-bucket
  18
  ;; needs work: v0 naming convention
  (let loop ((v0 v) (top? #t) (k canonize-and-maybe-intern-abstract-value))
   (cond
    ((and (eq? *mode* 'symbolic)
	  (not top?)
	  (not (inline-plus? (abstractify v0))))
     (let ((v-abstract (abstractify v0)))
      (k (new-call-unit (with-abstract (lambda () (plus v-abstract)))
			(c:builtin-name "plus" v-abstract)
			v))))
    ((unit? v0)
     (if (vlad-real? (unit-abstract-value v0))
	 (k (new-panic-unit "Argument to plus is not valid"))
	 (loop (unroll v0) top? k)))
    ((union? v0)
     (if (union-plus-cache v0)
	 (k (union-plus-cache v0))
	 (let ((v (create-tagged-union (union-tag v0) 'unfilled)))
	  (set-union-plus-cache! v0 v)
	  (map-cps-non-cs (lambda (v k) (loop v #f k))
			  (union-members v0)
			  (lambda (us)
			   (fill-union-values! v us)
			   (k v))))))
    ((vlad-pair? v0)
     (if (tagged-pair-plus-cache v0)
	 (k (tagged-pair-plus-cache v0))
	 (let ((v1 (vlad-car v0)) (v2 (vlad-cdr v0)))
	  (cond
	   ((and (unit? v1) (not (vlad-real? (unit-abstract-value v1))))
	    (loop (vlad-cons (unroll v1) v2) top? k))
	   ((and (unit? v2) (not (vlad-real? (unit-abstract-value v2))))
	    (loop (vlad-cons v1 (unroll v2)) top? k))
	   ((and (unit? v1)
		 (unit? v2)
		 (vlad-real? (unit-abstract-value v1))
		 (vlad-real? (unit-abstract-value v2)))
	    (k (new-+-unit v1 v2)))
	   ;; needs work: The following two don't check conformance.
	   ;; needs work: To figure out how to do this for the new code
	   ;;             generator.
	   ((and (not (eq? *mode* 'symbolic)) (is-zero? v1))
	    (set-tagged-pair-plus-cache! v0 v2)
	    (k v2))
	   ((and (not (eq? *mode* 'symbolic)) (is-zero? v2))
	    (set-tagged-pair-plus-cache! v0 v1)
	    (k v1))
	   ((union? v1)
	    (let ((v (create-tagged-union (union-tag v1) 'unfilled)))
	     (set-tagged-pair-plus-cache! v0 v)
	     (map-cps-non-cs (lambda (u1 k) (loop (vlad-cons u1 v2) #f k))
			     (union-members v1)
			     (lambda (us)
			      (fill-union-values! v us)
			      (k v)))))
	   ((union? v2)
	    (let ((v (create-tagged-union (union-tag v2) 'unfilled)))
	     (set-tagged-pair-plus-cache! v0 v)
	     (map-cps-non-cs (lambda (u2 k) (loop (vlad-cons v1 u2) #f k))
			     (union-members v2)
			     (lambda (us)
			      (fill-union-values! v us)
			      (k v)))))
	   ((and (vlad-empty-list? v1) (vlad-empty-list? v2))
	    (set-tagged-pair-plus-cache! v0 v1)
	    (k v1))
	   ((and (vlad-true? v1) (vlad-true? v2))
	    (set-tagged-pair-plus-cache! v0 v1)
	    (k v1))
	   ((and (vlad-false? v1) (vlad-false? v2))
	    (set-tagged-pair-plus-cache! v0 v1)
	    (k v1))
	   ((and (abstract-real? v1) (vlad-real? v2))
	    (set-tagged-pair-plus-cache! v0 v1)
	    (k v1))
	   ((and (vlad-real? v1) (abstract-real? v2))
	    (set-tagged-pair-plus-cache! v0 v2)
	    (k v2))
	   ((and (real? v1) (real? v2))
	    (let ((u (+ v1 v2)))
	     (set-tagged-pair-plus-cache! v0 u)
	     (k u)))
	   ((and
	     (primitive-procedure? v1) (primitive-procedure? v2) (eq? v1 v2))
	    (set-tagged-pair-plus-cache! v0 v1)
	    (k v1))
	   ((and (nonrecursive-closure? v1)
		 (nonrecursive-closure? v2)
		 (dereferenced-nonrecursive-closure-match? v1 v2))
	    ;; See the note in abstract-environment=?.
	    (let ((u (allocate-nonrecursive-closure
		      'unfilled (nonrecursive-closure-lambda-expression v1))))
	     (set-tagged-pair-plus-cache! v0 u)
	     (map2-cps-non-cs (lambda (v1 v2 k) (loop (vlad-cons v1 v2) #f k))
			      (get-nonrecursive-closure-values v1)
			      (get-nonrecursive-closure-values v2)
			      (lambda (vs)
			       (fill-nonrecursive-closure-values! u vs)
			       (k u)))))
	   ((and (recursive-closure? v1)
		 (recursive-closure? v2)
		 (dereferenced-recursive-closure-match? v1 v2))
	    ;; See the note in abstract-environment=?.
	    (let ((u (allocate-recursive-closure
		      'unfilled
		      (recursive-closure-procedure-variables v1)
		      (recursive-closure-lambda-expressions v1)
		      (recursive-closure-index v1))))
	     (set-tagged-pair-plus-cache! v0 u)
	     (map2-cps-non-cs (lambda (v1 v2 k) (loop (vlad-cons v1 v2) #f k))
			      (get-recursive-closure-values v1)
			      (get-recursive-closure-values v2)
			      (lambda (vs)
			       (fill-recursive-closure-values! u vs)
			       (k u)))))
	   ((and
	     (perturbation-tagged-value? v1) (perturbation-tagged-value? v2))
	    (let ((u (allocate-perturbation-tagged-value 'unfilled)))
	     (set-tagged-pair-plus-cache! v0 u)
	     (loop (vlad-cons (get-perturbation-tagged-value-primal v1)
			      (get-perturbation-tagged-value-primal v2))
		   #f
		   (lambda (v)
		    (fill-perturbation-tagged-value-primal! u v)
		    (k u)))))
	   ((and (bundle? v1) (bundle? v2))
	    (let ((u (allocate-bundle 'unfilled 'unfilled)))
	     (set-tagged-pair-plus-cache! v0 u)
	     (loop (vlad-cons (get-bundle-primal v1)
			      (get-bundle-primal v2))
		   #f
		   (lambda (v-primal)
		    (loop (vlad-cons (get-bundle-tangent v1)
				     (get-bundle-tangent v2))
			  #f
			  (lambda (v-tangent)
			   (fill-bundle! u v-primal v-tangent)
			   (k u)))))))
	   ((and (sensitivity-tagged-value? v1)
		 (sensitivity-tagged-value? v2))
	    (let ((u (allocate-sensitivity-tagged-value 'unfilled)))
	     (set-tagged-pair-plus-cache! v0 u)
	     (loop (vlad-cons (get-sensitivity-tagged-value-primal v1)
			      (get-sensitivity-tagged-value-primal v2))
		   #f
		   (lambda (v)
		    (fill-sensitivity-tagged-value-primal! u v)
		    (k u)))))
	   ((and (reverse-tagged-value? v1) (reverse-tagged-value? v2))
	    (let ((u (allocate-reverse-tagged-value 'unfilled)))
	     (set-tagged-pair-plus-cache! v0 u)
	     (loop (vlad-cons (get-reverse-tagged-value-primal v1)
			      (get-reverse-tagged-value-primal v2))
		   #f
		   (lambda (v)
		    (fill-reverse-tagged-value-primal! u v)
		    (k u)))))
	   ((and (tagged-pair? v1)
		 (tagged-pair? v2)
		 (equal-tags? (tagged-pair-tags v1) (tagged-pair-tags v2)))
	    (let ((u (allocate-tagged-pair
		      (tagged-pair-tags v1) 'unfilled 'unfilled)))
	     (set-tagged-pair-plus-cache! v0 u)
	     (loop (vlad-cons (get-tagged-pair-car v1)
			      (get-tagged-pair-car v2))
		   #f
		   (lambda (v-car)
		    (loop (vlad-cons (get-tagged-pair-cdr v1)
				     (get-tagged-pair-cdr v2))
			  #f
			  (lambda (v-cdr)
			   (fill-tagged-pair! u v-car v-cdr)
			   (k u)))))))
	   (else (case *mode*
		  ((concrete)
		   (run-time-error "Arguments to plus do not conform" v1 v2))
		  ((abstract)
		   (let ((u (compile-time-warning
			     "Arguments to plus might not conform" v1 v2)))
		    (set-tagged-pair-plus-cache! v0 u)
		    (k u)))
		  ((symbolic)
		   (k (new-panic-unit "Arguments to plus do not conform")))
		  (else (internal-error))))))))
    (else (k (ad-error "Argument to plus ~a valid" v0)))))))

(define (*j v)
 ;; This is written in CPS so as not to break structure sharing.
 (when (and *expensive-checks?* *interned?*)
  (check-canonize-cache! v)
  (check-intern-cache! v)
  (check-interned! v))
 (time-it-bucket
  19
  (let loop ((v v) (top? #t) (k canonize-and-maybe-intern-abstract-value))
   (cond
    ((and (eq? *mode* 'symbolic) (not top?) (not (inline-*j? (abstractify v))))
     (let ((v-abstract (abstractify v)))
      (k (new-call-unit (with-abstract (lambda () (*j v-abstract)))
			(c:builtin-name "starj" v-abstract)
			v))))
    ((unit? v)
     (if (vlad-real? (unit-abstract-value v))
	 (k (new-reverse-tagged-value v))
	 (loop (unroll v) top? k)))
    ((union? v)
     (if (union-*j-cache v)
	 (k (union-*j-cache v))
	 (let ((v-reverse (create-tagged-union (union-tag v) 'unfilled)))
	  (set-union-*j-inverse-cache! v-reverse v)
	  (set-union-*j-cache! v v-reverse)
	  (map-cps-non-cs (lambda (v k) (loop v #f k))
			  (union-members v)
			  (lambda (us-reverse)
			   (fill-union-values! v-reverse us-reverse)
			   (k v-reverse))))))
    ((vlad-empty-list? v) (k (new-reverse-tagged-value v)))
    ((vlad-true? v) (k (new-reverse-tagged-value v)))
    ((vlad-false? v) (k (new-reverse-tagged-value v)))
    ((vlad-real? v) (k (new-reverse-tagged-value v)))
    ((primitive-procedure? v) (k (primitive-procedure-reverse v)))
    ((nonrecursive-closure? v)
     (if (nonrecursive-closure-*j-cache v)
	 (k (nonrecursive-closure-*j-cache v))
	 ;; See the note in abstract-environment=?.
	 (let ((u-reverse
		(allocate-nonrecursive-closure
		 'unfilled
		 (reverse-transform
		  (nonrecursive-closure-lambda-expression v)))))
	  (set-nonrecursive-closure-*j-inverse-cache! u-reverse v)
	  (set-nonrecursive-closure-*j-cache! v u-reverse)
	  (map-cps-non-cs
	   (lambda (v k) (loop v #f k))
	   (get-nonrecursive-closure-values v)
	   (lambda (vs-reverse)
	    (fill-nonrecursive-closure-values! u-reverse vs-reverse)
	    (k u-reverse))))))
    ((recursive-closure? v)
     (if (recursive-closure-*j-cache v)
	 (k (recursive-closure-*j-cache v))
	 ;; See the note in abstract-environment=?.
	 (let ((u-reverse
		(allocate-recursive-closure
		 'unfilled
		 (map-vector
		  reverseify (recursive-closure-procedure-variables v))
		 (map-n-vector
		  (lambda (i)
		   (reverse-transform-internal
		    (vector-ref (recursive-closure-lambda-expressions v) i)
		    (vector->list (recursive-closure-procedure-variables v))
		    (vector->list (recursive-closure-lambda-expressions v))
		    i))
		  (vector-length (recursive-closure-lambda-expressions v)))
		 (recursive-closure-index v))))
	  (set-recursive-closure-*j-inverse-cache! u-reverse v)
	  (set-recursive-closure-*j-cache! v u-reverse)
	  (map-cps-non-cs
	   (lambda (v k) (loop v #f k))
	   (get-recursive-closure-values v)
	   (lambda (vs-reverse)
	    (fill-recursive-closure-values! u-reverse vs-reverse)
	    (k u-reverse))))))
    ((perturbation-tagged-value? v)
     (if (perturbation-tagged-value-*j-cache v)
	 (k (perturbation-tagged-value-*j-cache v))
	 (let ((u-reverse (create-reverse-tagged-value v)))
	  (set-perturbation-tagged-value-*j-cache! v u-reverse)
	  (k u-reverse))))
    ((bundle? v)
     (if (bundle-*j-cache v)
	 (k (bundle-*j-cache v))
	 (let ((u-reverse (create-reverse-tagged-value v)))
	  (set-bundle-*j-cache! v u-reverse)
	  (k u-reverse))))
    ((sensitivity-tagged-value? v)
     (if (sensitivity-tagged-value-*j-cache v)
	 (k (sensitivity-tagged-value-*j-cache v))
	 (let ((u-reverse (create-reverse-tagged-value v)))
	  (set-sensitivity-tagged-value-*j-cache! v u-reverse)
	  (k u-reverse))))
    ((reverse-tagged-value? v)
     (if (reverse-tagged-value-*j-cache v)
	 (k (reverse-tagged-value-*j-cache v))
	 (let ((u-reverse (create-reverse-tagged-value v)))
	  (set-reverse-tagged-value-*j-cache! v u-reverse)
	  (k u-reverse))))
    ((tagged-pair? v)
     (if (tagged-pair-*j-cache v)
	 (k (tagged-pair-*j-cache v))
	 (let ((u-reverse
		(allocate-tagged-pair
		 (add-tag 'reverse (tagged-pair-tags v)) 'unfilled 'unfilled)))
	  (set-tagged-pair-*j-inverse-cache! u-reverse v)
	  (set-tagged-pair-*j-cache! v u-reverse)
	  (loop
	   (get-tagged-pair-car v)
	   #f
	   (lambda (v-car-reverse)
	    (loop (get-tagged-pair-cdr v)
		  #f
		  (lambda (v-cdr-reverse)
		   (fill-tagged-pair! u-reverse v-car-reverse v-cdr-reverse)
		   (k u-reverse))))))))
    (else (internal-error))))))

(define (*j-inverse v-reverse)
 ;; This is written in CPS so as not to break structure sharing.
 (when (and *expensive-checks?* *interned?*)
  (check-canonize-cache! v-reverse)
  (check-intern-cache! v-reverse)
  (check-interned! v-reverse))
 (time-it-bucket
  20
  (let loop ((v-reverse v-reverse)
	     (top? #t)
	     (k canonize-and-maybe-intern-abstract-value))
   (cond
    ((and (eq? *mode* 'symbolic)
	  (not top?)
	  (not (inline-*j-inverse? (abstractify v-reverse))))
     (let ((v-reverse-abstract (abstractify v-reverse)))
      (k (new-call-unit
	  (with-abstract (lambda () (*j-inverse v-reverse-abstract)))
	  (c:builtin-name "starj_inverse" v-reverse-abstract)
	  v-reverse))))
    ((unit? v-reverse)
     (if (vlad-real? (unit-abstract-value v-reverse))
	 (k (new-panic-unit "Argument to unperturb is not a reverse value"))
	 (loop (unroll v-reverse) top? k)))
    ((union? v-reverse)
     (if (union-*j-inverse-cache v-reverse)
	 (k (union-*j-inverse-cache v-reverse))
	 (let ((v (create-tagged-union (union-tag v-reverse) 'unfilled)))
	  (set-union-*j-cache! v v-reverse)
	  (set-union-*j-inverse-cache! v-reverse v)
	  (map-cps-non-cs (lambda (v k) (loop v #f k))
			  (union-members v-reverse)
			  (lambda (us)
			   (fill-union-values! v us)
			   (k v))))))
    ((vlad-empty-list? v-reverse)
     (k (ad-error "Argument to *j-inverse ~a a reverse value" v-reverse)))
    ((vlad-true? v-reverse)
     (k (ad-error "Argument to *j-inverse ~a a reverse value" v-reverse)))
    ((vlad-false? v-reverse)
     (k (ad-error "Argument to *j-inverse ~a a reverse value" v-reverse)))
    ((vlad-real? v-reverse)
     (k (ad-error "Argument to *j-inverse ~a a reverse value" v-reverse)))
    ((primitive-procedure? v-reverse)
     (k (ad-error "Argument to *j-inverse ~a a reverse value" v-reverse)))
    ((nonrecursive-closure? v-reverse)
     (if (nonrecursive-closure-*j-inverse-cache v-reverse)
	 (k (nonrecursive-closure-*j-inverse-cache v-reverse))
	 (let ((b (find-if
		   (lambda (b)
		    (deep-abstract-value=?
		     v-reverse
		     (primitive-procedure-reverse (value-binding-value b))))
		   *value-bindings*)))
	  (cond
	   (b (let ((u (value-binding-value b)))
	       (set-nonrecursive-closure-*j-inverse-cache! v-reverse u)
	       (k u)))
	   ((tagged? 'reverse (nonrecursive-closure-tags v-reverse))
	    ;; See the note in abstract-environment=?.
	    (let ((u (allocate-nonrecursive-closure
		      'unfilled
		      (reverse-transform-inverse
		       (nonrecursive-closure-lambda-expression v-reverse)))))
	     (set-nonrecursive-closure-*j-cache! u v-reverse)
	     (set-nonrecursive-closure-*j-inverse-cache! v-reverse u)
	     (map-cps-non-cs (lambda (v k) (loop v #f k))
			     (get-nonrecursive-closure-values v-reverse)
			     (lambda (vs)
			      (fill-nonrecursive-closure-values! u vs)
			      (k u)))))
	   (else (k (ad-error "Argument to *j-inverse ~a a reverse value"
			      v-reverse)))))))
    ((recursive-closure? v-reverse)
     (cond ((recursive-closure-*j-inverse-cache v-reverse)
	    (k (recursive-closure-*j-inverse-cache v-reverse)))
	   ((tagged? 'reverse (recursive-closure-tags v-reverse))
	    ;; See the note in abstract-environment=?.
	    (let ((u (allocate-recursive-closure
		      'unfilled
		      (map-vector
		       unreverseify
		       (recursive-closure-procedure-variables v-reverse))
		      (map-vector
		       reverse-transform-inverse
		       (recursive-closure-lambda-expressions v-reverse))
		      (recursive-closure-index v-reverse))))
	     (set-recursive-closure-*j-cache! u v-reverse)
	     (set-recursive-closure-*j-inverse-cache! v-reverse u)
	     (map-cps-non-cs (lambda (v k) (loop v #f k))
			     (get-recursive-closure-values v-reverse)
			     (lambda (vs)
			      (fill-recursive-closure-values! u vs)
			      (k u)))))
	   (else (k (ad-error "Argument to *j-inverse ~a a reverse value"
			      v-reverse)))))
    ((perturbation-tagged-value? v-reverse)
     (k (ad-error "Argument to *j-inverse ~a a reverse value" v-reverse)))
    ((bundle? v-reverse)
     (k (ad-error "Argument to *j-inverse ~a a reverse value" v-reverse)))
    ((sensitivity-tagged-value? v-reverse)
     (k (ad-error "Argument to *j-inverse ~a a reverse value" v-reverse)))
    ((reverse-tagged-value? v-reverse)
     (k (get-reverse-tagged-value-primal v-reverse)))
    ((tagged-pair? v-reverse)
     (cond ((tagged-pair-*j-inverse-cache v-reverse)
	    (k (tagged-pair-*j-inverse-cache v-reverse)))
	   ((tagged? 'reverse (tagged-pair-tags v-reverse))
	    (let ((u (allocate-tagged-pair
		      (remove-tag 'reverse (tagged-pair-tags v-reverse))
		      'unfilled
		      'unfilled)))
	     (set-tagged-pair-*j-cache! u v-reverse)
	     (set-tagged-pair-*j-inverse-cache! v-reverse u)
	     (loop (get-tagged-pair-car v-reverse)
		   #f
		   (lambda (v-car)
		    (loop (get-tagged-pair-cdr v-reverse)
			  #f
			  (lambda (v-cdr)
			   (fill-tagged-pair! u v-car v-cdr)
			   (k u)))))))
	   (else (k (ad-error "Argument to *j-inverse ~a a reverse value"
			      v-reverse)))))
    (else (internal-error))))))

;;; Pretty printer

;;; *unabbreviate-executably?* assumes that:
;;;  1. you don't shadow *-primitve
;;;  2. shadowing doesn't occur because of the interning of uninterned symbols
;;;     that occurs implicitly by print followed by read

(define (externalize-expression e)
 (cond
  ((let*? e)
   (let loop ((ps (let*-parameters e)) (es (let*-expressions e)) (bs '()))
    (if (null? ps)
	(case (length bs)
	 ((0) (internal-error))
	 ((1) `(let ,bs ,(externalize-expression (let*-body e))))
	 (else `(let* ,(reverse bs) ,(externalize-expression (let*-body e)))))
	(loop (rest ps)
	      (rest es)
	      (cons `(,(externalize-expression (first ps))
		      ,(externalize-expression (first es)))
		    bs)))))
  ;; needs work: There are several problems with this rendering of constant
  ;;             expressions.
  ;;              1. primitive procedures, nonrecursive closures, recursive
  ;;                 closures, perturbation tagged values, bundles, sensitivity
  ;;                 tagged values, reverse tagged values, abstract booleans,
  ;;                 and abstract real cannot be read back in.
  ;;              2. This doesn't properly interface with unabbreviate-*
  ((constant-expression? e)
   (if (or (boolean? (constant-expression-value e))
	   (real? (constant-expression-value e)))
       (externalize (constant-expression-value e))
       `',(externalize (constant-expression-value e))))
  ((variable-access-expression? e)
   (variable-name (variable-access-expression-variable e)))
  ((lambda-expression? e)
   `(lambda (,(externalize-expression (lambda-expression-parameter e)))
     ,(externalize-expression (lambda-expression-body e))))
  ((application? e)
   `(,(externalize-expression (application-callee e))
     ,(externalize-expression (application-argument e))))
  ((letrec-expression? e)
   `(letrec ,(map (lambda (x e)
		   `(,(variable-name x) ,(externalize-expression e)))
		  (letrec-expression-procedure-variables e)
		  (letrec-expression-lambda-expressions e))
     ,(externalize-expression (letrec-expression-body e))))
  ((cons-expression? e)
   (if (empty-tags? (cons-expression-tags e))
       `(cons ,(externalize-expression (cons-expression-car e))
	      ,(externalize-expression (cons-expression-cdr e)))
       ;; needs work: This cannot be read back in.
       `(cons ,(cons-expression-tags e)
	      ,(externalize-expression (cons-expression-car e))
	      ,(externalize-expression (cons-expression-cdr e)))))
  (else (internal-error))))

(define (quotable? v)
 (cond ((and (not *unabbreviate-transformed?*) (perturbation-value? v)) #f)
       ((and (not *unabbreviate-transformed?*) (forward-value? v)) #f)
       ((and (not *unabbreviate-transformed?*) (sensitivity-value? v)) #f)
       ((and (not *unabbreviate-transformed?*) (reverse-value? v)) #f)
       ((vlad-empty-list? v) #t)
       ((vlad-true? v) #t)
       ((vlad-false? v) #t)
       ((real? v) #t)
       ((abstract-real? v) #f)
       ((vlad-pair? v) (and (quotable? (vlad-car v)) (quotable? (vlad-cdr v))))
       ((primitive-procedure? v) #f)
       ((closure? v) #f)
       ((perturbation-tagged-value? v) #f)
       ((bundle? v) #f)
       ((sensitivity-tagged-value? v) #f)
       ((reverse-tagged-value? v) #f)
       (else (internal-error))))

(define (debugging-externalize v)
 ;; breaks structure sharing
 (let loop ((v v) (vs '()))
  (cond
   ((memq v vs) `(up ,(positionq v vs)))
   ((union? v)
    (if (eq? (union-tag v) 'unfilled)
	(if (eq? (union-values v) 'unfilled)
	    '(union unfilled)
	    `(union
	      ,@(map (lambda (u) (loop u (cons v vs))) (union-members v))))
	(if (eq? (union-values v) 'unfilled)
	    `(tagged-union ,(loop (union-tag v) (cons v vs)) unfilled)
	    `(tagged-union ,(loop (union-tag v) (cons v vs))
			   ,@(map (lambda (u) (loop u (cons v vs)))
				  (get-union-values v))))))
   ((vlad-empty-list? v) '())
   ((vlad-true? v) #t)
   ((vlad-false? v) #f)
   ((real? v) v)
   ((abstract-real? v) v)
   ((primitive-procedure? v) (primitive-procedure-name v))
   ((nonrecursive-closure? v)
    (if (eq? (nonrecursive-closure-values v) 'unfilled)
	'(nonrecursive-closure unfilled)
	`(nonrecursive-closure
	  ,@(map (lambda (x v) `(,(variable-name x) ,(loop v vs)))
		 (nonrecursive-closure-variables v)
		 (get-nonrecursive-closure-values v)))))
   ((recursive-closure? v)
    (if (eq? (recursive-closure-values v) 'unfilled)
	`(recursive-closure
	  unfilled
	  ,(variable-name
	    (vector-ref (recursive-closure-procedure-variables v)
			(recursive-closure-index v))))
	`(recursive-closure
	  ,@(map (lambda (x v) `(,(variable-name x) ,(loop v vs)))
		 (recursive-closure-variables v)
		 (get-recursive-closure-values v))
	  ,(variable-name
	    (vector-ref (recursive-closure-procedure-variables v)
			(recursive-closure-index v))))))
   ((perturbation-tagged-value? v)
    (if (eq? (perturbation-tagged-value-primal v) 'unfilled)
	'(perturbation unfilled)
	`(perturbation ,(loop (get-perturbation-tagged-value-primal v) vs))))
   ((bundle? v)
    `(bundle ,(if (eq? (bundle-primal v) 'unfilled)
		  'unfilled
		  (loop (get-bundle-primal v) vs))
	     ,(if (eq? (bundle-tangent v) 'unfilled)
		  'unfilled
		  (loop (get-bundle-tangent v) vs))))
   ((sensitivity-tagged-value? v)
    (if (eq? (sensitivity-tagged-value-primal v) 'unfilled)
	'(sensitivity unfilled)
	`(sensitivity ,(loop (get-sensitivity-tagged-value-primal v) vs))))
   ((reverse-tagged-value? v)
    (if (eq? (reverse-tagged-value-primal v) 'unfilled)
	'(reverse unfilled)
	`(reverse ,(loop (get-reverse-tagged-value-primal v) vs))))
   ((tagged-pair? v) `(pair ,(tagged-pair-tags v)
			    ,(if (eq? (tagged-pair-car v) 'unfilled)
				 'unfilled
				 (loop (get-tagged-pair-car v) vs))
			    ,(if (eq? (tagged-pair-cdr v) 'unfilled)
				 'unfilled
				 (loop (get-tagged-pair-cdr v) vs))))
   ((name-unit? v) `(name ,(name-unit-code v)))
   ((call-unit? v) `(call ,(call-unit-code v)
			  ,@(map (lambda (x) (loop x vs)) (call-unit-xs v))))
   ((panic-unit? v) `(panic ,(panic-unit-x v)))
   ((+-unit? v) `(+ ,(loop (+-unit-x v) vs) ,(loop (+-unit-y v) vs)))
   ((--unit? v) `(- ,(loop (--unit-x v) vs) ,(loop (--unit-y v) vs)))
   ((*-unit? v) `(* ,(loop (*-unit-x v) vs) ,(loop (*-unit-y v) vs)))
   ((/-unit? v) `(/ ,(loop (/-unit-x v) vs) ,(loop (/-unit-y v) vs)))
   ((sqrt-unit? v) `(sqrt ,(loop (sqrt-unit-x v) vs)))
   ((exp-unit? v) `(exp ,(loop (exp-unit-x v) vs)))
   ((log-unit? v) `(log ,(loop (log-unit-x v) vs)))
   ((sin-unit? v) `(sin ,(loop (sin-unit-x v) vs)))
   ((cos-unit? v) `(cos ,(loop (cos-unit-x v) vs)))
   ((atan-unit? v)
    `(atan ,(loop (atan-unit-x v) vs) ,(loop (atan-unit-y v) vs)))
   ((=-unit? v) `(= ,(loop (=-unit-x v) vs) ,(loop (=-unit-y v) vs)))
   ((<-unit? v) `(< ,(loop (<-unit-x v) vs) ,(loop (<-unit-y v) vs)))
   ((>-unit? v) `(> ,(loop (>-unit-x v) vs) ,(loop (>-unit-y v) vs)))
   ((<=-unit? v) `(<= ,(loop (<=-unit-x v) vs) ,(loop (<=-unit-y v) vs)))
   ((>=-unit? v) `(>= ,(loop (>=-unit-x v) vs) ,(loop (>=-unit-y v) vs)))
   ((zero?-unit? v) `(zero? ,(loop (zero?-unit-x v) vs)))
   ((positive?-unit? v) `(positive? ,(loop (positive?-unit-x v) vs)))
   ((negative?-unit? v) `(negative? ,(loop (negative?-unit-x v) vs)))
   ((read-real-unit? v) `(read-real ,(loop (read-real-unit-x v) vs)))
   ((write-real-unit? v) `(write-real ,(loop (write-real-unit-x v) vs)))
   (else (internal-error)))))

(define (externalize v)
 ;; breaks structure sharing
 (let ((v
	(let loop ((v v) (quote? #f) (vs '()))
	 (cond
	  ((memq v vs)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(up ,(positionq v vs)))
	  ((and (union? v)
		(eq? (union-tag v) 'unfilled)
		(= (length (get-union-values v)) 2)
		(some vlad-empty-list? (get-union-values v))
		(some (lambda (u)
		       (and (tagged-pair? u)
			    (empty-tags? (tagged-pair-tags u))
			    (deep-abstract-value=? (tagged-pair-cdr u) v)))
		      (get-union-values v)))
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(list*
	     ,(loop
	       (tagged-pair-car
		(find-if (lambda (u)
			  (and (tagged-pair? u)
			       (empty-tags? (tagged-pair-tags u))
			       (deep-abstract-value=? (tagged-pair-cdr u) v)))
			 (get-union-values v)))
	       quote?
	       vs)))
	  ((union? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   (cond ((empty-abstract-value? v) 'bottom)
		 ((null? (rest (union-members v))) (internal-error))
		 ((eq? (union-tag v) 'unfilled)
		  `(union ,@(map (lambda (u) (loop u quote? (cons v vs)))
				 (union-members v))))
		 (else `(tagged-union
			 ,(loop (union-tag v) quote? (cons v vs))
			 ,@(map (lambda (u) (loop u quote? (cons v vs)))
				(get-union-values v))))))
	  ((and (or (not *unabbreviate-transformed?*) (tagged-pair? v))
		(perturbation-value? v))
	   (cond (*unabbreviate-executably?*
		  (assert (not quote?))
		  ;; needs work: These calls to unperturb won't work for
		  ;;             symbolic values.
		  `(perturb ,(loop (unperturb v) quote? vs)))
		 (else `(perturbation ,(loop (unperturb v) quote? vs)))))
	  ((and (or (not *unabbreviate-transformed?*) (tagged-pair? v))
		(forward-value? v))
	   (cond (*unabbreviate-executably?*
		  (assert (not quote?))
		  ;; needs work: These calls to primal and tangent won't work
		  ;;             for symbolic values.
		  `(bundle ,(loop (primal v) quote? vs)
			   ,(loop (tangent v) quote? vs)))
		 (else `(forward ,(loop (primal v) quote? vs)
				 ,(loop (tangent v) quote? vs)))))
	  ((and (or (not *unabbreviate-transformed?*) (tagged-pair? v))
		(sensitivity-value? v)
		;; needs work: This call to unsensitize? won't work for
		;;             symbolic values.
		;; This is to prevent attempts to unsensitize backpropagators.
		(unsensitize? v))
	   (cond (*unabbreviate-executably?*
		  (assert (not quote?))
		  ;; needs work: These calls to unsensitize won't work for
		  ;;             symbolic values.
		  `(sensitize ,(loop (unsensitize v) quote? vs)))
		 (else `(sensitivity ,(loop (unsensitize v) quote? vs)))))
	  ;; It may not be possible to apply *j-inverse to a closure whose
	  ;; parameter is reverse tagged. Such a situation arises when you
	  ;; externalize an analysis. It may contain closures that result from
	  ;; lambda expressions that correspond to tails of anf forms of lambda
	  ;; expression bodies.
	  ((and (or (not *unabbreviate-transformed?*) (tagged-pair? v))
		(reverse-value? v))
	   (cond (*unabbreviate-executably?*
		  (assert (not quote?))
		  ;; needs work: These calls to *j-inverse won't work for
		  ;;             symbolic values.
		  `(*j ,(loop (*j-inverse v) quote? vs)))
		 (else `(reverse ,(loop (*j-inverse v) quote? vs)))))
	  ((vlad-empty-list? v)
	   (if (and *unabbreviate-executably?* (not quote?)) ''() '()))
	  ((vlad-true? v) #t)
	  ((vlad-false? v) #f)
	  ((real? v) v)
	  ((abstract-real? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   v)
	  ((vlad-pair? v)
	   (if (and *unabbreviate-executably?* (not quote?))
	       (if (quotable? v)
		   `',(cons (loop (vlad-car v) #t vs)
			    (loop (vlad-cdr v) #t vs))
		   `(cons ,(loop (vlad-car v) #f vs)
			  ,(loop (vlad-cdr v) #f vs)))
	       (cons (loop (vlad-car v) quote? vs)
		     (loop (vlad-cdr v) quote? vs))))
	  ((primitive-procedure? v)
	   (cond (*unabbreviate-executably?*
		  (assert (not quote?))
		  (string->symbol
		   (string-append (symbol->string (primitive-procedure-name v))
				  (symbol->string '-primitive))))
		 (else (primitive-procedure-name v))))
	  ((nonrecursive-closure? v)
	   (cond
	    (*unabbreviate-executably?*
	     (assert (not quote?))
	     (case (length (nonrecursive-closure-variables v))
	      ((0) (externalize-expression
		    (nonrecursive-closure-lambda-expression v)))
	      ((1) `(let ,(map (lambda (x v)
				`(,(variable-name x) ,(loop v quote? vs)))
			       (nonrecursive-closure-variables v)
			       (get-nonrecursive-closure-values v))
		     ,(externalize-expression
		       (nonrecursive-closure-lambda-expression v))))
	      (else `(let ,(map (lambda (x v)
				 `(,(variable-name x) ,(loop v quote? vs)))
				(nonrecursive-closure-variables v)
				(get-nonrecursive-closure-values v))
		      ,(externalize-expression
			(nonrecursive-closure-lambda-expression v))))))
	    (*unabbreviate-nonrecursive-closures?*
	     `(nonrecursive-closure
	       ,(map (lambda (x v) `(,(variable-name x) ,(loop v quote? vs)))
		     (nonrecursive-closure-variables v)
		     (get-nonrecursive-closure-values v))
	       ,(externalize-expression
		 (nonrecursive-closure-lambda-expression v))))
	    (else (externalize-expression
		   (nonrecursive-closure-lambda-expression v)))))
	  ((recursive-closure? v)
	   (cond
	    (*unabbreviate-executably?*
	     (assert (not quote?))
	     (case (length (recursive-closure-variables v))
	      ((0) `(letrec ,(vector->list
			      (map-vector
			       (lambda (x e)
				`(,(variable-name x)
				  ,(externalize-expression e)))
			       (recursive-closure-procedure-variables v)
			       (recursive-closure-lambda-expressions v)))
		     ,(variable-name
		       (vector-ref (recursive-closure-procedure-variables v)
				   (recursive-closure-index v)))))
	      ((1) `(let ,(map (lambda (x v)
				`(,(variable-name x) ,(loop v quote? vs)))
			       (recursive-closure-variables v)
			       (get-recursive-closure-values v))
		     (letrec ,(vector->list
			       (map-vector
				(lambda (x e)
				 `(,(variable-name x)
				   ,(externalize-expression e)))
				(recursive-closure-procedure-variables v)
				(recursive-closure-lambda-expressions v)))
		      ,(variable-name
			(vector-ref (recursive-closure-procedure-variables v)
				    (recursive-closure-index v))))))
	      (else
	       `(let ,(map (lambda (x v)
			    `(,(variable-name x) ,(loop v quote? vs)))
			   (recursive-closure-variables v)
			   (get-recursive-closure-values v))
		 (letrec ,(vector->list
			   (map-vector
			    (lambda (x e)
			     `(,(variable-name x) ,(externalize-expression e)))
			    (recursive-closure-procedure-variables v)
			    (recursive-closure-lambda-expressions v)))
		  ,(variable-name
		    (vector-ref (recursive-closure-procedure-variables v)
				(recursive-closure-index v))))))))
	    (*unabbreviate-recursive-closures?*
	     `(recursive-closure
	       ,(map (lambda (x v) `(,(variable-name x) ,(loop v quote? vs)))
		     (recursive-closure-variables v)
		     (get-recursive-closure-values v))
	       ,(vector->list
		 (map-vector
		  (lambda (x e)
		   `(,(variable-name x) ,(externalize-expression e)))
		  (recursive-closure-procedure-variables v)
		  (recursive-closure-lambda-expressions v)))
	       ,(variable-name
		 (vector-ref (recursive-closure-procedure-variables v)
			     (recursive-closure-index v)))))
	    (else (variable-name
		   (vector-ref (recursive-closure-procedure-variables v)
			       (recursive-closure-index v))))))
	  ((perturbation-tagged-value? v)
	   ;; Only needed for *unabbreviate-transformed?*.
	   (cond
	    (*unabbreviate-executably?*
	     (assert (not quote?))
	     `(perturb
	       ,(loop (get-perturbation-tagged-value-primal v) quote? vs)))
	    (else
	     `(perturbation
	       ,(loop (get-perturbation-tagged-value-primal v) quote? vs)))))
	  ((bundle? v)
	   ;; Only needed for *unabbreviate-transformed?*.
	   (cond (*unabbreviate-executably?*
		  (assert (not quote?))
		  `(bundle ,(loop (get-bundle-primal v) quote? vs)
			   ,(loop (get-bundle-tangent v) quote? vs)))
		 (else `(forward ,(loop (get-bundle-primal v) quote? vs)
				 ,(loop (get-bundle-tangent v) quote? vs)))))
	  ((sensitivity-tagged-value? v)
	   ;; Only needed for *unabbreviate-transformed?*.
	   (cond
	    (*unabbreviate-executably?*
	     (assert (not quote?))
	     `(sensitize
	       ,(loop (get-sensitivity-tagged-value-primal v) quote? vs)))
	    (else
	     `(sensitivity
	       ,(loop (get-sensitivity-tagged-value-primal v) quote? vs)))))
	  ((reverse-tagged-value? v)
	   ;; Only needed for *unabbreviate-transformed?*.
	   (cond
	    (*unabbreviate-executably?*
	     (assert (not quote?))
	     `(*j ,(loop (get-reverse-tagged-value-primal v) quote? vs)))
	    (else `(reverse
		    ,(loop (get-reverse-tagged-value-primal v) quote? vs)))))
	  ((name-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(name ,(name-unit-code v)))
	  ((call-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(call ,(call-unit-code v)
		  ,@(map (lambda (x) (loop x quote? vs))
			 (call-unit-xs v))))
	  ((panic-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(panic ,(panic-unit-x v)))
	  ((+-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(+ ,(loop (+-unit-x v) quote? vs) ,(loop (+-unit-y v) quote? vs)))
	  ((--unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(- ,(loop (--unit-x v) quote? vs) ,(loop (--unit-y v) quote? vs)))
	  ((*-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(* ,(loop (*-unit-x v) quote? vs) ,(loop (*-unit-y v) quote? vs)))
	  ((/-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(/ ,(loop (/-unit-x v) quote? vs) ,(loop (/-unit-y v) quote? vs)))
	  ((sqrt-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(sqrt ,(loop (sqrt-unit-x v) quote? vs)))
	  ((exp-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(exp ,(loop (exp-unit-x v) quote? vs)))
	  ((log-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(log ,(loop (log-unit-x v) quote? vs)))
	  ((sin-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(sin ,(loop (sin-unit-x v) quote? vs)))
	  ((cos-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(cos ,(loop (cos-unit-x v) quote? vs)))
	  ((atan-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(atan ,(loop (atan-unit-x v) quote? vs)
		  ,(loop (atan-unit-y v) quote? vs)))
	  ((=-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(= ,(loop (=-unit-x v) quote? vs) ,(loop (=-unit-y v) quote? vs)))
	  ((<-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(< ,(loop (<-unit-x v) quote? vs) ,(loop (<-unit-y v) quote? vs)))
	  ((>-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(> ,(loop (>-unit-x v) quote? vs) ,(loop (>-unit-y v) quote? vs)))
	  ((<=-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(<= ,(loop (<=-unit-x v) quote? vs)
		,(loop (<=-unit-y v) quote? vs)))
	  ((>=-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(>= ,(loop (>=-unit-x v) quote? vs)
		,(loop (>=-unit-y v) quote? vs)))
	  ((zero?-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(zero? ,(loop (zero?-unit-x v) quote? vs)))
	  ((positive?-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(positive? ,(loop (positive?-unit-x v) quote? vs)))
	  ((negative?-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(negative? ,(loop (negative?-unit-x v) quote? vs)))
	  ((read-real-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(read-real ,(loop (read-real-unit-x v) quote? vs)))
	  ((write-real-unit? v)
	   (when *unabbreviate-executably?*
	    (run-time-error "Cannot unabbreviate executably" v))
	   `(write-real ,(loop (write-real-unit-x v) quote? vs)))
	  (else (internal-error))))))
  (if *unabbreviate-executably?*
      `(let* ,(map (lambda (b)
		    (let ((x (variable-name (value-binding-variable b))))
		     `(,(string->symbol
			 (string-append (symbol->string x)
					(symbol->string '-primitive)))
		       ,x)))
		   *value-bindings*)
	,v)
      v)))

(define (externalize-environment xs vs)
 (assert (and (list? vs) (= (length xs) (length vs))))
 (map (lambda (x v) (list (variable-name x) (externalize v))) xs vs))

(define (externalize-environment-binding xs b)
 (assert (environment-binding? b))
 (list (externalize-environment xs (environment-binding-values b))
       (externalize (environment-binding-value b))))

(define (externalize-environment-bindings xs bs)
 (assert (and (list? bs) (every environment-binding? bs)))
 (map (lambda (b) (externalize-environment-binding xs b)) bs))

(define (externalize-analysis)
 (map (lambda (e)
       (list (externalize-expression e)
	     (externalize-environment-bindings
	      (free-variables e)
	      (expression-environment-bindings e))))
      (remove-if (lambda (e) (null? (expression-environment-bindings e)))
		 *expressions*)))

;;; Concrete Evaluator

(define (with-write-level n thunk)
 (let ((m (write-level)))
  (set-write-level! n)
  (thunk)
  (set-write-level! m)))

(define (with-write-length n thunk)
 (let ((m (write-length)))
  (set-write-length! n)
  (thunk)
  (set-write-length! m)))

;;; Environment Restriction/Construction

(define (restrict-environment vs is)
 (let ((vs (list->vector vs))) (map (lambda (i) (vector-ref vs i)) is)))

(define (letrec-nested-environment vs e)
 ;; The abstract values in vs might violate the syntactic constraints. We adopt
 ;; the constraint that all abstract values in all environment bindings satisfy
 ;; the syntactic constraints. We widen here so that we compare widened values
 ;; to widened values.
 (map (lambda (x)
       (if (memp variable=? x (letrec-expression-procedure-variables e))
	   ;; This may create an abstract value that violates the syntactic
	   ;; constraints.
	   (new-recursive-closure
	    (restrict-environment vs (letrec-expression-indices e))
	    (list->vector (letrec-expression-procedure-variables e))
	    (list->vector (letrec-expression-lambda-expressions e))
	    (positionp variable=? x (letrec-expression-procedure-variables e)))
	   (list-ref vs (positionp variable=? x (free-variables e)))))
      (free-variables (letrec-expression-body e))))

(define (concrete-destructure p v)
 (cond
  ((constant-expression? p)
   (unless (abstract-value=? (constant-expression-value p) v)
    (run-time-error (format #f "Argument is not an equivalent value for ~s"
			    (externalize-expression p))
		    v))
   '())
  ((variable-access-expression? p)
   (list (cons (variable-access-expression-variable p) v)))
  ((lambda-expression? p)
   (unless (and (nonrecursive-closure? v)
		(dereferenced-expression-eqv?
		 p (nonrecursive-closure-lambda-expression v)))
    (run-time-error
     (format #f "Argument is not a matching nonrecursive closure for ~s"
	     (externalize-expression p))
     v))
   (map cons (parameter-variables p) (get-nonrecursive-closure-values v)))
  ((letrec-expression? p)
   (assert (and (variable-access-expression? (letrec-expression-body p))
		(memp variable=?
		      (variable-access-expression-variable
		       (letrec-expression-body p))
		      (letrec-expression-procedure-variables p))))
   (unless (and (recursive-closure? v)
		(= (recursive-closure-index v)
		   (positionp variable=?
			      (variable-access-expression-variable
			       (letrec-expression-body p))
			      (letrec-expression-procedure-variables p)))
		(= (vector-length
		    (recursive-closure-procedure-variables v))
		   (length (letrec-expression-procedure-variables p)))
		(= (vector-length
		    (recursive-closure-lambda-expressions v))
		   (length (letrec-expression-lambda-expressions p)))
		(every
		 dereferenced-expression-eqv?
		 (vector->list (recursive-closure-lambda-expressions v))
		 (letrec-expression-lambda-expressions p)))
    (run-time-error
     (format #f "Argument is not a matching recursive closure for ~s"
	     (externalize-expression p))
     v))
   (map cons (parameter-variables p) (get-recursive-closure-values v)))
  ((cons-expression? p)
   (unless (and (tagged-pair? v)
		(equal-tags? (cons-expression-tags p) (tagged-pair-tags v)))
    (run-time-error
     (format #f "Argument is not a matching tagged pair with tags ~s"
	     (cons-expression-tags p))
     v))
   (append
    (concrete-destructure (cons-expression-car p) (get-tagged-pair-car v))
    (concrete-destructure (cons-expression-cdr p) (get-tagged-pair-cdr v))))
  (else (internal-error))))

(define (construct-environment-for-let e vs alist)
 (map (lambda (x)
       (let ((result (assp variable=? x alist)))
	(if result
	    (cdr result)
	    (list-ref vs (positionp variable=? x (free-variables e))))))
      (free-variables (lambda-expression-body (application-callee e)))))

(define (construct-environment u1 alist)
 ;; We don't need to enforce the constraint that the abstract values in the
 ;; result environment not violate the syntactic constraints since the result
 ;; environment is only passed to abstract-eval1, abstract-eval-prime!,
 ;; all-instances1-instances2, generate-letrec-bindings, and
 ;; generate-expression and the constraint is enforced there.
 (assert (not (union? u1)))
 (cond
  ((nonrecursive-closure? u1)
   (map
    (lambda (x)
     (let ((result (assp variable=? x alist)))
      (if result
	  (cdr result)
	  (list-ref
	   (get-nonrecursive-closure-values u1)
	   (positionp variable=? x (nonrecursive-closure-variables u1))))))
    (free-variables
     (lambda-expression-body (nonrecursive-closure-lambda-expression u1)))))
  ((recursive-closure? u1)
   (map (lambda (x)
	 (let ((result (assp variable=? x alist)))
	  (cond
	   (result (cdr result))
	   ((some-vector (lambda (x1) (variable=? x x1))
			 (recursive-closure-procedure-variables u1))
	    ;; This may create an abstract value that violates the syntactic
	    ;; constraints.
	    (new-recursive-closure
	     (get-recursive-closure-values u1)
	     (recursive-closure-procedure-variables u1)
	     (recursive-closure-lambda-expressions u1)
	     (positionp-vector
	      variable=? x (recursive-closure-procedure-variables u1))))
	   (else
	    (list-ref
	     (get-recursive-closure-values u1)
	     (positionp variable=? x (recursive-closure-variables u1)))))))
	(free-variables (lambda-expression-body
			 (vector-ref (recursive-closure-lambda-expressions u1)
				     (recursive-closure-index u1))))))
  (else (internal-error))))

;;; needs work: This evaluator is not tail recursive.

(define (concrete-apply v1 v2)
 (unless (vlad-procedure? v1) (run-time-error "Target is not a procedure" v1))
 (unless (prefix-tags? (value-tags v1) (value-tags v2))
  (run-time-error "Argument has wrong type for target" v1 v2))
 (set! *stack* (cons (list v1 v2) *stack*))
 (when (cond ((primitive-procedure? v1) *trace-primitive-procedures?*)
	     ((nonrecursive-closure? v1) *trace-nonrecursive-closures?*)
	     ((recursive-closure? v1) *trace-recursive-closures?*)
	     (else (internal-error)))
  (if *trace-argument/result?*
      (format #t "~aentering ~s ~s~%"
	      (make-string *trace-level* #\space)
	      (externalize v1)
	      (externalize v2))
      (format #t "~aentering ~s~%"
	      (make-string *trace-level* #\space)
	      (externalize v1)))
  (set! *trace-level* (+ *trace-level* 1)))
 (when (and *metered?* (primitive-procedure? v1))
  (set-primitive-procedure-meter!
   v1 (+ (primitive-procedure-meter v1) 1)))
 (let ((result
	(cond
	 ((primitive-procedure? v1) ((primitive-procedure-concrete v1) v2))
	 ((closure? v1)
	  (concrete-eval
	   (closure-body v1)
	   (construct-environment
	    v1 (concrete-destructure (closure-parameter v1) v2))))
	 (else (internal-error)))))
  (set! *stack* (rest *stack*))
  (when (cond ((primitive-procedure? v1) *trace-primitive-procedures?*)
	      ((nonrecursive-closure? v1) *trace-nonrecursive-closures?*)
	      ((recursive-closure? v1) *trace-recursive-closures?*)
	      (else (internal-error)))
   (set! *trace-level* (- *trace-level* 1))
   (if *trace-argument/result?*
       (format #t "~aexiting ~s ~s~%"
	       (make-string *trace-level* #\space)
	       (externalize v1)
	       (externalize result))
       (format #t "~aexiting ~s~%"
	       (make-string *trace-level* #\space)
	       (externalize v1))))
  result))

(define (concrete-eval e vs)
 (cond
  ((constant-expression? e) (constant-expression-value e))
  ((variable-access-expression? e) (first vs))
  ((lambda-expression? e) (new-nonrecursive-closure vs e))
  ((application? e)
   (if (lambda-expression? (application-callee e))
       ;; This handling of LET is an optimization. It affects the stack trace
       ;; on error, tracing, and the tag-check error message.
       (let* ((e1 (application-callee e))
	      (v (concrete-eval
		  (application-argument e)
		  (restrict-environment vs (application-argument-indices e)))))
	(unless (prefix-tags? (lambda-expression-tags e1) (value-tags v))
	 (run-time-error "Value has wrong type for let binder" v))
	(concrete-eval
	 (lambda-expression-body e1)
	 (construct-environment-for-let
	  e vs (concrete-destructure (lambda-expression-parameter e1) v))))
       ;; This LET* is to specify the evaluation order.
       (let* ((v1 (concrete-eval
		   (application-callee e)
		   (restrict-environment vs (application-callee-indices e))))
	      (v2 (concrete-eval
		   (application-argument e)
		   (restrict-environment
		    vs (application-argument-indices e)))))
	(concrete-apply v1 v2))))
  ((letrec-expression? e)
   (concrete-eval (letrec-expression-body e) (letrec-nested-environment vs e)))
  ((cons-expression? e)
   ;; This LET* is to specify the evaluation order.
   (let* ((v1 (concrete-eval
	       (cons-expression-car e)
	       (restrict-environment vs (cons-expression-car-indices e))))
	  (v2 (concrete-eval
	       (cons-expression-cdr e)
	       (restrict-environment vs (cons-expression-cdr-indices e)))))
    (unless (prefix-tags? (cons-expression-tags e) (value-tags v1))
     (run-time-error
      (format #f "CAR argument has wrong type for target with tags ~s"
	      (cons-expression-tags e))
      v1))
    (unless (prefix-tags? (cons-expression-tags e) (value-tags v2))
     (run-time-error
      (format #f "CDR argument has wrong type for target with tags ~s"
	      (cons-expression-tags e))
      v2))
    (new-tagged-pair (cons-expression-tags e) v1 v2)))
  (else (internal-error))))

;;; Flow Analysis

;;; Abstract Values

(define (concrete-value->abstract-value v)
 ;; breaks structure sharing
 (cond
  ((scalar-value? v)
   (if (and *imprecise-inexacts?* (real? v) (inexact? v)) (abstract-real) v))
  ((nonrecursive-closure? v)
   (new-nonrecursive-closure
    (map concrete-value->abstract-value (get-nonrecursive-closure-values v))
    (nonrecursive-closure-lambda-expression v)))
  ((recursive-closure? v)
   (new-recursive-closure
    (map concrete-value->abstract-value (get-recursive-closure-values v))
    (recursive-closure-procedure-variables v)
    (recursive-closure-lambda-expressions v)
    (recursive-closure-index v)))
  ((perturbation-tagged-value? v)
   (new-perturbation-tagged-value
    (concrete-value->abstract-value (get-perturbation-tagged-value-primal v))))
  ((bundle? v)
   (new-bundle (concrete-value->abstract-value (get-bundle-primal v))
	       (concrete-value->abstract-value (get-bundle-tangent v))))
  ((sensitivity-tagged-value? v)
   (new-sensitivity-tagged-value
    (concrete-value->abstract-value (get-sensitivity-tagged-value-primal v))))
  ((reverse-tagged-value? v)
   (new-reverse-tagged-value
    (concrete-value->abstract-value (get-reverse-tagged-value-primal v))))
  ((tagged-pair? v)
   (new-tagged-pair (tagged-pair-tags v)
		    (concrete-value->abstract-value (get-tagged-pair-car v))
		    (concrete-value->abstract-value (get-tagged-pair-cdr v))))
  (else (internal-error))))

;;; Widen

;;; Width

(define (reduce-real-width limit v)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  21
  (canonize-and-maybe-intern-abstract-value
   (let loop ((v v) (cs '()) (k (lambda (v-prime cs) v-prime)))
    (let ((found? (assq v cs)))
     (cond
      (found? (k (cdr found?) cs))
      ((union? v)
       (let ((v-prime (allocate-union 'unfilled)))
	(map-cps loop
		 (if (> (count-if real? (union-members v)) limit)
		     (cons (abstract-real) (remove-if real? (union-members v)))
		     (union-members v))
		 (cons (cons v v-prime) cs)
		 (lambda (us-prime cs)
		  (fill-union-values! v-prime us-prime)
		  (k v-prime cs)))))
      ((vlad-empty-list? v)
       (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
      ((vlad-true? v)
       (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
      ((vlad-false? v)
       (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
      ((vlad-real? v)
       (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
      ((primitive-procedure? v)
       (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
      ((nonrecursive-closure? v)
       ;; See the note in abstract-environment=?.
       (let ((u-prime (allocate-nonrecursive-closure
		       'unfilled (nonrecursive-closure-lambda-expression v))))
	(map-cps loop
		 (get-nonrecursive-closure-values v)
		 (cons (cons v u-prime) cs)
		 (lambda (vs-prime cs)
		  (fill-nonrecursive-closure-values! u-prime vs-prime)
		  (k u-prime cs)))))
      ((recursive-closure? v)
       ;; See the note in abstract-environment=?.
       (let ((u-prime (allocate-recursive-closure
		       'unfilled
		       (recursive-closure-procedure-variables v)
		       (recursive-closure-lambda-expressions v)
		       (recursive-closure-index v))))
	(map-cps loop
		 (get-recursive-closure-values v)
		 (cons (cons v u-prime) cs)
		 (lambda (vs-prime cs)
		  (fill-recursive-closure-values! u-prime vs-prime)
		  (k u-prime cs)))))
      ((perturbation-tagged-value? v)
       (let ((u-prime (allocate-perturbation-tagged-value 'unfilled)))
	(loop (get-perturbation-tagged-value-primal v)
	      (cons (cons v u-prime) cs)
	      (lambda (v-prime cs)
	       (fill-perturbation-tagged-value-primal! u-prime v-prime)
	       (k u-prime cs)))))
      ((bundle? v)
       (let ((u-prime (allocate-bundle 'unfilled 'unfilled)))
	(loop (get-bundle-primal v)
	      (cons (cons v u-prime) cs)
	      (lambda (v-primal-prime cs)
	       (loop (get-bundle-tangent v)
		     cs
		     (lambda (v-tangent-prime cs)
		      (fill-bundle! u-prime v-primal-prime v-tangent-prime)
		      (k u-prime cs)))))))
      ((sensitivity-tagged-value? v)
       (let ((u-prime (allocate-sensitivity-tagged-value 'unfilled)))
	(loop (get-sensitivity-tagged-value-primal v)
	      (cons (cons v u-prime) cs)
	      (lambda (v-prime cs)
	       (fill-sensitivity-tagged-value-primal! u-prime v-prime)
	       (k u-prime cs)))))
      ((reverse-tagged-value? v)
       (let ((u-prime (allocate-reverse-tagged-value 'unfilled)))
	(loop (get-reverse-tagged-value-primal v)
	      (cons (cons v u-prime) cs)
	      (lambda (v-prime cs)
	       (fill-reverse-tagged-value-primal! u-prime v-prime)
	       (k u-prime cs)))))
      ((tagged-pair? v)
       (let ((u-prime
	      (allocate-tagged-pair (tagged-pair-tags v) 'unfilled 'unfilled)))
	(loop (get-tagged-pair-car v)
	      (cons (cons v u-prime) cs)
	      (lambda (v-car-prime cs)
	       (loop (get-tagged-pair-cdr v)
		     cs
		     (lambda (v-cdr-prime cs)
		      (fill-tagged-pair! u-prime v-car-prime v-cdr-prime)
		      (k u-prime cs)))))))
      (else (internal-error))))))))

(define (limit-real-width v)
 (if (eq? *real-width-limit* #f) v (reduce-real-width *real-width-limit* v)))

(define (pick-values-to-coalesce-for-width-limit limit match? type? v)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  22
  (let outer ((v v) (vs '()) (k (lambda (vs) #f)))
   (cond
    ((real? v) (k vs))
    ((memq v vs) (k vs))
    ((union? v)
     (let* ((us (find-if (lambda (us) (> (length us) limit))
			 (transitive-equivalence-classesp
			  match? (remove-if-not type? (union-members v))))))
      (if us
	  (list (first us) (second us))
	  (let inner ((us (union-members v)) (vs (cons v vs)))
	   (if (null? us)
	       (k vs)
	       (outer (first us) vs (lambda (vs) (inner (rest us) vs))))))))
    ((scalar-value? v) (k vs))
    (else
     (let inner ((vs1 (aggregate-value-values v)) (vs (cons v vs)))
      (if (null? vs1)
	  (k vs)
	  (outer (first vs1) vs (lambda (vs) (inner (rest vs1) vs))))))))))

(define (merge-subabstract-values v u1 u2)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  23
  (canonize-and-maybe-intern-abstract-value
   (let ((u12 (create-aggregate-value-with-new-values
	       u1
	       (map abstract-value-union
		    (aggregate-value-values u1)
		    (aggregate-value-values u2)))))
    (let loop ((v v) (cs '()) (k (lambda (v-prime cs) v-prime)))
     (let ((found? (assq v cs)))
      (cond
       (found? (k (cdr found?) cs))
       ;; needs work: Do we need to update cs here?
       ((or (eq? v u1) (eq? v u2)) (loop u12 cs k))
       ((union? v)
	(let ((v-prime (allocate-union 'unfilled)))
	 (map-cps loop
		  (union-members v)
		  (cons (cons v v-prime) cs)
		  (lambda (us-prime cs)
		   (fill-union-values! v-prime us-prime)
		   (k v-prime cs)))))
       ((vlad-empty-list? v)
	(let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
       ((vlad-true? v)
	(let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
       ((vlad-false? v)
	(let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
       ((vlad-real? v)
	(let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
       ((primitive-procedure? v)
	(let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
       ((nonrecursive-closure? v)
	;; See the note in abstract-environment=?.
	(let ((u-prime (allocate-nonrecursive-closure
			'unfilled (nonrecursive-closure-lambda-expression v))))
	 (map-cps loop
		  (get-nonrecursive-closure-values v)
		  (cons (cons v u-prime) cs)
		  (lambda (vs-prime cs)
		   (fill-nonrecursive-closure-values! u-prime vs-prime)
		   (k u-prime cs)))))
       ((recursive-closure? v)
	;; See the note in abstract-environment=?.
	(let ((u-prime (allocate-recursive-closure
			'unfilled
			(recursive-closure-procedure-variables v)
			(recursive-closure-lambda-expressions v)
			(recursive-closure-index v))))
	 (map-cps loop
		  (get-recursive-closure-values v)
		  (cons (cons v u-prime) cs)
		  (lambda (vs-prime cs)
		   (fill-recursive-closure-values! u-prime vs-prime)
		   (k u-prime cs)))))
       ((perturbation-tagged-value? v)
	(let ((u-prime (allocate-perturbation-tagged-value 'unfilled)))
	 (loop (get-perturbation-tagged-value-primal v)
	       (cons (cons v u-prime) cs)
	       (lambda (v-prime cs)
		(fill-perturbation-tagged-value-primal! u-prime v-prime)
		(k u-prime cs)))))
       ((bundle? v)
	(let ((u-prime (allocate-bundle 'unfilled 'unfilled)))
	 (loop (get-bundle-primal v)
	       (cons (cons v u-prime) cs)
	       (lambda (v-primal-prime cs)
		(loop (get-bundle-tangent v)
		      cs
		      (lambda (v-tangent-prime cs)
		       (fill-bundle! u-prime v-primal-prime v-tangent-prime)
		       (k u-prime cs)))))))
       ((sensitivity-tagged-value? v)
	(let ((u-prime (allocate-sensitivity-tagged-value 'unfilled)))
	 (loop (get-sensitivity-tagged-value-primal v)
	       (cons (cons v u-prime) cs)
	       (lambda (v-prime cs)
		(fill-sensitivity-tagged-value-primal! u-prime v-prime)
		(k u-prime cs)))))
       ((reverse-tagged-value? v)
	(let ((u-prime (allocate-reverse-tagged-value 'unfilled)))
	 (loop (get-reverse-tagged-value-primal v)
	       (cons (cons v u-prime) cs)
	       (lambda (v-prime cs)
		(fill-reverse-tagged-value-primal! u-prime v-prime)
		(k u-prime cs)))))
       ((tagged-pair? v)
	(let ((u-prime (allocate-tagged-pair
			(tagged-pair-tags v) 'unfilled 'unfilled)))
	 (loop (get-tagged-pair-car v)
	       (cons (cons v u-prime) cs)
	       (lambda (v-car-prime cs)
		(loop (get-tagged-pair-cdr v)
		      cs
		      (lambda (v-cdr-prime cs)
		       (fill-tagged-pair! u-prime v-car-prime v-cdr-prime)
		       (k u-prime cs)))))))
       (else (internal-error)))))))))

(define (limit-width limit match? type? v)
 (if (eq? limit #f)
     v
     (let loop ((v v))
      (let ((u1-u2
	     (pick-values-to-coalesce-for-width-limit limit match? type? v)))
       (if (eq? u1-u2 #f)
	   v
	   (let* ((v-prime
		   (merge-subabstract-values v (first u1-u2) (second u1-u2))))
	    ;; See note in limit-depth.
	    (assert (abstract-value-subset? v v-prime))
	    (loop v-prime)))))))

;;; Depth

;;; A path is a list of abstract values. The first element of the list is the
;;; root and the last element is a leaf. The last element is either a scalar
;;; abstract value, an aggregate abstract value that has no children, or an up.
;;; Each abstract value is a slot or a member of the preceeding abstract value.

(define (path-of-greatest-depth match? type? v)
 ;; This is written in CPS so as not to break structure sharing.
 ;; We now adopt a more efficient representation of paths. A path is a set of
 ;; sets of abstract values. Each abstract values satisfies type? and each set
 ;; of abstract values is an equivalence class by match?. The depth is thus the
 ;; cardinality of the largest equivalence class.
 (time-it-bucket
  24
  (let outer ((v v)
	      (cs '())
	      (path '())
	      (depth-of-path 0)
	      (longest-path #f)
	      (depth-of-longest-path #f)
	      (k
	       (lambda (longest-path depth-of-longest-path cs) longest-path)))
   (let ((found? (assq v cs)))
    (cond
     (found?
      (if (> depth-of-path (cdr found?))
	  (if (some (lambda (class) (memq v class)) path)
	      (k (if (or (eq? longest-path #f)
			 (> depth-of-path depth-of-longest-path))
		     path
		     longest-path)
		 (if (or (eq? longest-path #f)
			 (> depth-of-path depth-of-longest-path))
		     depth-of-path
		     depth-of-longest-path)
		 (map (lambda (c)
		       (if (eq? (car c) v) (cons v depth-of-path) c))
		      cs))
	      (outer v
		     (remove-if (lambda (c) (eq? (car c) v)) cs)
		     path
		     depth-of-path
		     longest-path
		     depth-of-longest-path
		     k))
	  (k longest-path depth-of-longest-path cs)))
     ((union? v)
      ;; This assumes that unions never contribute to depth.
      (let inner ((us (get-union-values v))
		  (cs (cons (cons v depth-of-path) cs))
		  (longest-path
		   (if (or (eq? longest-path #f)
			   (> depth-of-path depth-of-longest-path))
		       path
		       longest-path))
		  (depth-of-longest-path
		   (if (or (eq? longest-path #f)
			   (> depth-of-path depth-of-longest-path))
		       depth-of-path
		       depth-of-longest-path)))
       (if (null? us)
	   (k longest-path depth-of-longest-path cs)
	   (outer
	    (first us)
	    cs
	    path
	    depth-of-path
	    longest-path
	    depth-of-longest-path
	    (lambda (longest-path depth-of-longest-path cs)
	     (inner (rest us) cs longest-path depth-of-longest-path))))))
     ((scalar-value? v)
      ;; This assumes that scalars never contribute to depth.
      (k (if (or (eq? longest-path #f) (> depth-of-path depth-of-longest-path))
	     path
	     longest-path)
	 (if (or (eq? longest-path #f) (> depth-of-path depth-of-longest-path))
	     depth-of-path
	     depth-of-longest-path)
	 (cons (cons v depth-of-path) cs)))
     (else
      ;; This assumes that only values of type? contribute to depth.
      (let* ((path (if (type? v)
		       (let loop ((path path))
			(cond ((null? path) (list (list v)))
			      ;; This assumes that match? is transitive.
			      ((match? v (first (first path)))
			       (cons (cons v (first path)) (rest path)))
			      (else (cons (first path) (loop (rest path))))))
		       path))
	     (depth-of-path (map-reduce max 0 length path)))
       (let inner ((vs (aggregate-value-values v))
		   (cs (cons (cons v depth-of-path) cs))
		   (longest-path
		    (if (or (eq? longest-path #f)
			    (> depth-of-path depth-of-longest-path))
			path
			longest-path))
		   (depth-of-longest-path
		    (if (or (eq? longest-path #f)
			    (> depth-of-path depth-of-longest-path))
			depth-of-path
			depth-of-longest-path)))
	(if (null? vs)
	    (k longest-path depth-of-longest-path cs)
	    (outer
	     (first vs)
	     cs
	     path
	     depth-of-path
	     longest-path
	     depth-of-longest-path
	     (lambda (longest-path depth-of-longest-path cs)
	      (inner
	       (rest vs) cs longest-path depth-of-longest-path))))))))))))

(define (path-of-depth-greater-than-limit limit match? type? v)
 (let ((longest-path (path-of-greatest-depth match? type? v)))
  (if (and (not (eq? longest-path #f))
	   (> (map-reduce max 0 length longest-path) limit))
      longest-path
      #f)))

(define (pick-values-to-coalesce-for-depth-limit path)
 (let* ((k (map-reduce max 0 length path))
	;; We arbitrarily pick the first class.
	(class (find-if (lambda (class) (= (length class) k)) path)))
  ;; We arbitrarily pick the first two members of the class.
  (list (first class) (second class))))

(define (limit-depth limit match? type? v)
 (if (or (eq? limit #f)
	 (let ((vs (type-in-abstract-value type? v)))
	  (or (<= (length vs) limit)
	      (every (lambda (vs) (<= (length vs) limit))
		     (transitive-equivalence-classesp match? vs)))))
     v
     (let loop ((v v))
      (let ((path (path-of-depth-greater-than-limit limit match? type? v)))
       (if (eq? path #f)
	   v
	   (let* ((u1-u2 (pick-values-to-coalesce-for-depth-limit path))
		  (v-prime
		   (merge-subabstract-values v (first u1-u2) (second u1-u2))))
	    ;; -all-limits 1 on {cannon,backprop}-{F,R} trigger this. I believe
	    ;; this is because of the conservative nature of
	    ;; abstract-value-subset? but I haven't fully checked that.
	    ;; (assert (abstract-value-subset? v v-prime))
	    (loop v-prime)))))))

;;; List widening

(define (widen-lists v)
 ;; This is written in CPS so as not to break structure sharing.
 (if *widen-lists?*
     (time-it-bucket
      25
      (canonize-and-maybe-intern-abstract-value
       (let loop ((v v) (cs '()) (k (lambda (v-prime cs) v-prime)))
	(let ((found? (assq v cs)))
	 (cond
	  (found? (k (cdr found?) cs))
	  ((union? v)
	   (let ((v-prime (allocate-union 'unfilled)))
	    (map-cps loop
		     (union-members v)
		     (cons (cons v v-prime) cs)
		     (lambda (us-prime cs)
		      (fill-union-values! v-prime us-prime)
		      (k v-prime cs)))))
	  ((vlad-empty-list? v)
	   (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
	  ((vlad-true? v)
	   (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
	  ((vlad-false? v)
	   (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
	  ((vlad-real? v)
	   (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
	  ((primitive-procedure? v)
	   (let ((u-prime v)) (k u-prime (cons (cons v u-prime) cs))))
	  ((nonrecursive-closure? v)
	   ;; See the note in abstract-environment=?.
	   (let ((u-prime (allocate-nonrecursive-closure
			   'unfilled
			   (nonrecursive-closure-lambda-expression v))))
	    (map-cps loop
		     (get-nonrecursive-closure-values v)
		     (cons (cons v u-prime) cs)
		     (lambda (vs-prime cs)
		      (fill-nonrecursive-closure-values! u-prime vs-prime)
		      (k u-prime cs)))))
	  ((recursive-closure? v)
	   ;; See the note in abstract-environment=?.
	   (let ((u-prime (allocate-recursive-closure
			   'unfilled
			   (recursive-closure-procedure-variables v)
			   (recursive-closure-lambda-expressions v)
			   (recursive-closure-index v))))
	    (map-cps loop
		     (get-recursive-closure-values v)
		     (cons (cons v u-prime) cs)
		     (lambda (vs-prime cs)
		      (fill-recursive-closure-values! u-prime vs-prime)
		      (k u-prime cs)))))
	  ((perturbation-tagged-value? v)
	   (let ((u-prime (allocate-perturbation-tagged-value 'unfilled)))
	    (loop (get-perturbation-tagged-value-primal v)
		  (cons (cons v u-prime) cs)
		  (lambda (v-prime cs)
		   (fill-perturbation-tagged-value-primal! u-prime v-prime)
		   (k u-prime cs)))))
	  ((bundle? v)
	   (let ((u-prime (allocate-bundle 'unfilled 'unfilled)))
	    (loop (get-bundle-primal v)
		  (cons (cons v u-prime) cs)
		  (lambda (v-primal-prime cs)
		   (loop (get-bundle-tangent v)
			 cs
			 (lambda (v-tangent-prime cs)
			  (fill-bundle! u-prime v-primal-prime v-tangent-prime)
			  (k u-prime cs)))))))
	  ((sensitivity-tagged-value? v)
	   (let ((u-prime (allocate-sensitivity-tagged-value 'unfilled)))
	    (loop (get-sensitivity-tagged-value-primal v)
		  (cons (cons v u-prime) cs)
		  (lambda (v-prime cs)
		   (fill-sensitivity-tagged-value-primal! u-prime v-prime)
		   (k u-prime cs)))))
	  ((reverse-tagged-value? v)
	   (let ((u-prime (allocate-reverse-tagged-value 'unfilled)))
	    (loop (get-reverse-tagged-value-primal v)
		  (cons (cons v u-prime) cs)
		  (lambda (v-prime cs)
		   (fill-reverse-tagged-value-primal! u-prime v-prime)
		   (k u-prime cs)))))
	  ((tagged-pair? v)
	   (cond
	    ;; See note in abstract-value-unionable?.
	    ((tagged-empty-list? (tagged-pair-tags v) (tagged-pair-cdr v))
	     (let* ((u-prime (allocate-tagged-pair
			      (tagged-pair-tags v) 'unfilled 'unfilled))
		    (v-prime
		     (allocate-union (list u-prime (tagged-pair-cdr v)))))
	      (loop (get-tagged-pair-car v)
		    cs
		    (lambda (v-car-prime cs)
		     (fill-tagged-pair! u-prime v-car-prime v-prime)
		     (k v-prime cs)))))
	    ((and (union? (tagged-pair-cdr v))
		  (= (length (union-members (tagged-pair-cdr v))) 2)
		  (some (lambda (u)
			 (tagged-empty-list? (tagged-pair-tags v) u))
			(union-members (tagged-pair-cdr v)))
		  (some (lambda (u)
			 (and (tagged-pair? u)
			      (equal-tags? (tagged-pair-tags u)
					   (tagged-pair-tags v))
			      (deep-abstract-value=? (tagged-pair-cdr u)
						     (tagged-pair-cdr v))))
			(union-members (tagged-pair-cdr v)))
		  (abstract-value-unionable?
		   #t
		   (get-tagged-pair-car v)
		   (get-tagged-pair-car
		    (find-if
		     (lambda (u)
		      (and (tagged-pair? u)
			   (equal-tags? (tagged-pair-tags u)
					(tagged-pair-tags v))
			   (deep-abstract-value=? (tagged-pair-cdr u)
						  (tagged-pair-cdr v))))
		     (union-members (tagged-pair-cdr v))))))
	     (let* ((u-prime (allocate-tagged-pair
			      (tagged-pair-tags v) 'unfilled 'unfilled))
		    (v-prime
		     (allocate-union
		      (list
		       u-prime
		       (find-if (lambda (u)
				 (tagged-empty-list? (tagged-pair-tags v) u))
				(union-members (tagged-pair-cdr v)))))))
	      (loop (abstract-value-union-internal
		     (get-tagged-pair-car v)
		     (get-tagged-pair-car
		      (find-if
		       (lambda (u)
			(and (tagged-pair? u)
			     (equal-tags? (tagged-pair-tags u)
					  (tagged-pair-tags v))
			     (deep-abstract-value=? (tagged-pair-cdr u)
						    (tagged-pair-cdr v))))
		       (union-members (tagged-pair-cdr v)))))
		    cs
		    (lambda (v-car-prime cs)
		     (fill-tagged-pair! u-prime v-car-prime v-prime)
		     (k v-prime cs)))))
	    (else
	     (let ((u-prime (allocate-tagged-pair
			     (tagged-pair-tags v) 'unfilled 'unfilled)))
	      (loop (get-tagged-pair-car v)
		    (cons (cons v u-prime) cs)
		    (lambda (v-car-prime cs)
		     (loop (get-tagged-pair-cdr v)
			   cs
			   (lambda (v-cdr-prime cs)
			    (fill-tagged-pair! u-prime v-car-prime v-cdr-prime)
			    (k u-prime cs)))))))))
	  (else (internal-error)))))))
     v))

;;; Syntactic Constraints

(define (limit-closure-width v)
 (limit-width *closure-width-limit* closure-match? closure? v))

(define (limit-perturbation-tagged-value-width v)
 (limit-width *perturbation-tagged-value-width-limit*
	      (lambda (u1 u2) #t)
	      perturbation-tagged-value?
	      v))

(define (limit-bundle-width v)
 (limit-width *bundle-width-limit* (lambda (u1 u2) #t) bundle? v))

(define (limit-sensitivity-tagged-value-width v)
 (limit-width *sensitivity-tagged-value-width-limit*
	      (lambda (u1 u2) #t)
	      sensitivity-tagged-value?
	      v))

(define (limit-reverse-tagged-value-width v)
 (limit-width *reverse-tagged-value-width-limit*
	      (lambda (u1 u2) #t)
	      reverse-tagged-value?
	      v))

(define (limit-tagged-pair-width v)
 (limit-width *tagged-pair-width-limit*
	      (lambda (u1 u2)
	       (equal-tags? (tagged-pair-tags u1) (tagged-pair-tags u2)))
	      tagged-pair?
	      v))

(define (backpropagator? u)
 ;; needs work: This is a kludge and might not work because some
 ;;             backpropagators might be unsensitizable.
 (and (nonrecursive-closure? u)
      (not (null? (value-tags u)))
      ;; An optimization
      (memq 'sensitivity (value-tags u))
      (case (first (value-tags u))
       ((perturbation) (backpropagator? (unperturb u)))
       ((forward) (backpropagator? (primal u)))
       ((sensitivity)
	(or (not (unsensitize? u)) (backpropagator? (unsensitize u))))
       ((reverse) (backpropagator? (*j-inverse u)))
       (else (internal-error)))))

(define (backpropagator-variable? x)
 (let loop ((x (variable-name x)))
  (or (and (list? x)
	   (= (length x) 3)
	   (eq? (first x) 'alpha)
	   (loop (second x))
	   (integer? (third x))
	   (exact? (third x))
	   (not (negative? (third x))))
      (and (list? x)
	   (= (length x) 2)
	   (eq? (first x) 'backpropagator)
	   (integer? (second x))
	   (exact? (second x))
	   (not (negative? (second x))))
      (and (list? x)
	   (= (length x) 2)
	   (eq? (first x) 'perturbation)
	   (loop (second x)))
      (and (list? x)
	   (= (length x) 2)
	   (eq? (first x) 'forward)
	   (loop (second x)))
      (and (list? x)
	   (= (length x) 2)
	   (eq? (first x) 'sensitivity)
	   (loop (second x)))
      (and (list? x)
	   (= (length x) 2)
	   (eq? (first x) 'reverse)
	   (loop (second x))))))

(define (backpropagator-match? u1 u2)
 (and
  (nonrecursive-closure-match? u1 u2)
  (every (lambda (v1 v2) (abstract-value-unionable? #f v1 v2))
	 (get-nonrecursive-closure-values u1)
	 (get-nonrecursive-closure-values u2))
  (let ((p?s (map abstract-value=?
		  (get-nonrecursive-closure-values u1)
		  (get-nonrecursive-closure-values u2))))
   (and (= (countq #t p?s) (- (length (get-nonrecursive-closure-values u1)) 1))
	(backpropagator-variable?
	 (list-ref (free-variables (nonrecursive-closure-lambda-expression u1))
		   (positionq #f p?s)))
	(backpropagator-variable?
	 (list-ref (free-variables (nonrecursive-closure-lambda-expression u2))
		   (positionq #f p?s)))))))

(define (limit-backpropagator-depth v)
 (limit-depth
  *backpropagator-depth-limit* backpropagator-match? backpropagator? v))

(define (limit-closure-depth v)
 (limit-depth *closure-depth-limit* closure-match? closure? v))

(define (limit-perturbation-tagged-value-depth v)
 (limit-depth *perturbation-tagged-value-depth-limit*
	      (lambda (u1 u2) #t)
	      perturbation-tagged-value?
	      v))

(define (limit-bundle-depth v)
 (limit-depth *bundle-depth-limit* (lambda (u1 u2) #t) bundle? v))

(define (limit-sensitivity-tagged-value-depth v)
 (limit-depth *sensitivity-tagged-value-depth-limit*
	      (lambda (u1 u2) #t)
	      sensitivity-tagged-value?
	      v))

(define (limit-reverse-tagged-value-depth v)
 (limit-depth *reverse-tagged-value-depth-limit*
	      (lambda (u1 u2) #t)
	      reverse-tagged-value?
	      v))

(define (limit-tagged-pair-depth v)
 (limit-depth *tagged-pair-depth-limit*
	      (lambda (u1 u2)
	       (equal-tags? (tagged-pair-tags u1) (tagged-pair-tags u2)))
	      tagged-pair?
	      v))

(define (widen-abstract-value v)
 (let ((v (canonize-and-maybe-intern-abstract-value v)))
  (cond
   ((and (nonrecursive-closure? v) (nonrecursive-closure-widen v))
    (nonrecursive-closure-widen v))
   ((and (recursive-closure? v) (recursive-closure-widen v))
    (recursive-closure-widen v))
   ((and (perturbation-tagged-value? v) (perturbation-tagged-value-widen v))
    (perturbation-tagged-value-widen v))
   ((and (bundle? v) (bundle-widen v)) (bundle-widen v))
   ((and (sensitivity-tagged-value? v) (sensitivity-tagged-value-widen v))
    (sensitivity-tagged-value-widen v))
   ((and (reverse-tagged-value? v) (reverse-tagged-value-widen v))
    (reverse-tagged-value-widen v))
   ((and (tagged-pair? v) (tagged-pair-widen v)) (tagged-pair-widen v))
   ((and (union? v) (union-widen v)) (union-widen v))
   (else
    (let ((v-prime
	   (let loop ((v v))
	    (let ((v-prime (widen-lists
			    (limit-tagged-pair-depth
			     (limit-reverse-tagged-value-depth
			      (limit-sensitivity-tagged-value-depth
			       (limit-bundle-depth
				(limit-perturbation-tagged-value-depth
				 (limit-backpropagator-depth
				  (limit-closure-depth
				   (limit-tagged-pair-width
				    (limit-reverse-tagged-value-width
				     (limit-sensitivity-tagged-value-width
				      (limit-bundle-width
				       (limit-perturbation-tagged-value-width
					(limit-closure-width v))))))))))))))))
	     (if (abstract-value=? v v-prime)
		 (let ((v-prime (limit-real-width v)))
		  (assert (abstract-value-subset? v v-prime))
		  v-prime)
		 (loop v-prime))))))
     (cond
      ((nonrecursive-closure? v) (set-nonrecursive-closure-widen! v v-prime))
      ((recursive-closure? v) (set-recursive-closure-widen! v v-prime))
      ((perturbation-tagged-value? v)
       (set-perturbation-tagged-value-widen! v v-prime))
      ((bundle? v) (set-bundle-widen! v v-prime))
      ((sensitivity-tagged-value? v)
       (set-sensitivity-tagged-value-widen! v v-prime))
      ((reverse-tagged-value? v) (set-reverse-tagged-value-widen! v v-prime))
      ((tagged-pair? v) (set-tagged-pair-widen! v v-prime))
      ((union? v) (set-union-widen! v v-prime)))
     v-prime)))))

;;; Abstract Evaluator

(define (abstract-eval1 e vs)
 ;; The abstract values in vs might violate the syntactic constraints. We adopt
 ;; the constraint that all abstract values in all environment bindings satisfy
 ;; the syntactic constraints. We widen here so that we compare widened values
 ;; to widened values.
 (let ((vs (map widen-abstract-value vs)))
  (assert (<= (count-if
	       (lambda (b)
		(abstract-environment=? vs (environment-binding-values b)))
	       (expression-environment-bindings e))
	      1))
  (let ((b (find-if
	    (lambda (b)
	     (abstract-environment=? vs (environment-binding-values b)))
	    (expression-environment-bindings e))))
   (if b (environment-binding-value b) (empty-abstract-value)))))

(define (abstract-destructure p v)
 ;; The assumption is that v doesn't violate the syntactic constraints.
 (cond
  ;; This case comes first to avoid the dispatch.
  ((variable-access-expression? p)
   (list (list (cons (variable-access-expression-variable p) v))))
  ((union? v)
   (map-reduce
    append '() (lambda (u) (abstract-destructure p u)) (union-members v)))
  ((constant-expression? p)
   (cond
    ((abstract-value=?
      ;; This can widen when the constant expression value violates the
      ;; syntactic constraints (presumably tagged pair depth limit). This would
      ;; correspond to the call A to c:widen in generate-destructure.
      (widen-abstract-value
       (concrete-value->abstract-value (constant-expression-value p)))
      v)
     '(()))
    ((abstract-value-nondisjoint?
      ;; This can widen when the constant expression value violates the
      ;; syntactic constraints (presumably tagged pair depth limit). This would
      ;; correspond to the call A to c:widen in generate-destructure.
      (widen-abstract-value
       (concrete-value->abstract-value (constant-expression-value p)))
      v)
     (compile-time-warning
      (format #f "Argument might not be an equivalent value for ~s"
	      (constant-expression-value p))
      v)
     '(()))
    (else (compile-time-warning
	   (format #f "Argument might not be an equivalent value for ~s"
		   (constant-expression-value p))
	   v)
	  '())))
  ((lambda-expression? p)
   (cond
    ((and (nonrecursive-closure? v)
	  (dereferenced-expression-eqv?
	   p (nonrecursive-closure-lambda-expression v)))
     (list
      (map cons (parameter-variables p) (get-nonrecursive-closure-values v))))
    (else
     (compile-time-warning
      (format #f "Argument might not be a matching nonrecursive closure for ~s"
	      (externalize-expression p))
      v)
     '())))
  ((letrec-expression? p)
   (assert (and (variable-access-expression? (letrec-expression-body p))
		(memp variable=?
		      (variable-access-expression-variable
		       (letrec-expression-body p))
		      (letrec-expression-procedure-variables p))))
   (cond
    ((and (recursive-closure? v)
	  (= (recursive-closure-index v)
	     (positionp variable=?
			(variable-access-expression-variable
			 (letrec-expression-body p))
			(letrec-expression-procedure-variables p)))
	  (= (vector-length
	      (recursive-closure-procedure-variables v))
	     (length (letrec-expression-procedure-variables p)))
	  (= (vector-length
	      (recursive-closure-lambda-expressions v))
	     (length (letrec-expression-lambda-expressions p)))
	  (every dereferenced-expression-eqv?
		 (vector->list (recursive-closure-lambda-expressions v))
		 (letrec-expression-lambda-expressions p)))
     (list
      (map cons (parameter-variables p) (get-recursive-closure-values v))))
    (else
     (compile-time-warning
      (format #f "Argument might not be a matching recursive closure for ~s"
	      (externalize-expression p))
      v)
     '())))
  ((cons-expression? p)
   (cond
    ((and (tagged-pair? v)
	  (equal-tags? (cons-expression-tags p) (tagged-pair-tags v)))
     (cross-product
      append
      (abstract-destructure (cons-expression-car p) (get-tagged-pair-car v))
      (abstract-destructure (cons-expression-cdr p) (get-tagged-pair-cdr v))))
    (else
     (compile-time-warning
      (format #f "Argument might not be a matching tagged pair with tags ~s"
	      (cons-expression-tags p))
      v)
     '())))
  (else (internal-error))))

(define (abstract-apply v1 v2)
 (if (empty-abstract-value? v2)
     v2
     (map-union
      (lambda (u1)
       (cond
	((primitive-procedure? u1) ((primitive-procedure-abstract u1) v2))
	((closure? u1)
	 (cond
	  ((every-value-tags
	    (lambda (tags2) (prefix-tags? (value-tags u1) tags2)) v2)
	   (unionize
	    (map (lambda (alist)
		  (abstract-eval1
		   (closure-body u1) (construct-environment u1 alist)))
		 (abstract-destructure (closure-parameter u1) v2))))
	  ((some-value-tags
	    (lambda (tags2) (prefix-tags? (value-tags u1) tags2)) v2)
	   (compile-time-warning
	    "Argument might have wrong type for target" u1 v2)
	   (unionize
	    (map (lambda (alist)
		  (abstract-eval1
		   (closure-body u1) (construct-environment u1 alist)))
		 (abstract-destructure (closure-parameter u1) v2))))
	  (else (compile-time-warning
		 "Argument might have wrong type for target" u1 v2))))
	(else (compile-time-warning "Target might not be a procedure" u1))))
      v1)))

(define (enqueue! e)
 (unless (expression-enqueue? e)
  (set-expression-enqueue?! e #t)
  (set! *queue* (cons e *queue*))))

(define (abstract-eval! e)
 (cond
  ((application? e)
   (cond
    ((lambda-expression? (application-callee e))
     ;; This handling of LET is an optimization. See the note in concrete-eval.
     (let ((e1 (lambda-expression-body (application-callee e)))
	   (p (lambda-expression-parameter (application-callee e)))
	   (tags1 (lambda-expression-tags (application-callee e))))
      (for-each
       (lambda (b)
	(let* ((vs (environment-binding-values b))
	       (v (abstract-eval1 (application-argument e)
				  (restrict-environment
				   vs (application-argument-indices e)))))
	 (unless (empty-abstract-value? v)
	  (cond
	   ((every-value-tags (lambda (tags) (prefix-tags? tags1 tags)) v)
	    (for-each
	     (lambda (alist)
	      ;; See the note in abstract-eval-prime!
	      ;; here I am: Can hoist this since it doesn't depend on alist
	      ;;            or b. It only depends on alist being nonempty and
	      ;;            v not being an empty abstract value.
	      (unless (memp expression-eqv? e (expression-parents e1))
	       (set-expression-parents! e1 (cons e (expression-parents e1)))
	       (enqueue! e))
	      (abstract-eval-prime!
	       e1 (construct-environment-for-let e vs alist)))
	     (abstract-destructure p v)))
	   ((some-value-tags (lambda (tags) (prefix-tags? tags1 tags)) v)
	    (compile-time-warning
	     "Value might have wrong type for let binder" v)
	    (for-each
	     (lambda (alist)
	      ;; See the note in abstract-eval-prime!
	      ;; here I am: Can hoist this since it doesn't depend on alist
	      ;;            or b. It only depends on alist being nonempty and
	      ;;            v not being an empty abstract value.
	      (unless (memp expression-eqv? e (expression-parents e1))
	       (set-expression-parents! e1 (cons e (expression-parents e1)))
	       (enqueue! e))
	      (abstract-eval-prime!
	       e1 (construct-environment-for-let e vs alist)))
	     (abstract-destructure p v)))
	   (else (compile-time-warning
		  "Value might have wrong type for let binder" v))))))
       (expression-environment-bindings e))
      (for-each
       (lambda (b)
	(let* ((vs (environment-binding-values b))
	       (v (abstract-eval1 (application-argument e)
				  (restrict-environment
				   vs (application-argument-indices e))))
	       ;; This corresponds to call B-prime to c:widen in
	       ;; generate-expression.
	       (v (widen-abstract-value
		   ;; Need to refresh my memory as to why this union is needed.
		   (abstract-value-union
		    (environment-binding-value b)
		    (cond
		     ((every-value-tags
		       (lambda (tags) (prefix-tags? tags1 tags)) v)
		      (unionize
		       (map (lambda (alist)
			     (abstract-eval1
			      e1 (construct-environment-for-let e vs alist)))
			    (abstract-destructure p v))))
		     ((some-value-tags
		       (lambda (tags) (prefix-tags? tags1 tags)) v)
		      (compile-time-warning
		       "Value might have wrong type for let binder" v)
		      (unionize
		       (map (lambda (alist)
			     (abstract-eval1
			      e1 (construct-environment-for-let e vs alist)))
			    (abstract-destructure p v))))
		     (else
		      (compile-time-warning
		       "Value might have wrong type for let binder" v)))))))
	 ;; With the above union the old value will always be a subset of the
	 ;; new value by a precise calculation but might not be given that the
	 ;; subset calculation is imprecise. Need to document example where
	 ;; this occurs.
	 (unless (abstract-value-subset? v (environment-binding-value b))
	  (set-environment-binding-value! b v)
	  (for-each enqueue! (expression-parents e)))))
       (expression-environment-bindings e))))
    (else
     (for-each (lambda (b)
		(abstract-apply-prime!
		 e
		 (abstract-eval1
		  (application-callee e)
		  (restrict-environment (environment-binding-values b)
					(application-callee-indices e)))
		 (abstract-eval1
		  (application-argument e)
		  (restrict-environment (environment-binding-values b)
					(application-argument-indices e)))))
	       (expression-environment-bindings e))
     (for-each
      (lambda (b)
       ;; This corresponds to call B to c:widen in generate-expression.
       (let ((v (widen-abstract-value
		 ;; Need to refresh my memory as to why this union is needed.
		 (abstract-value-union
		  (environment-binding-value b)
		  (abstract-apply
		   (abstract-eval1 (application-callee e)
				   (restrict-environment
				    (environment-binding-values b)
				    (application-callee-indices e)))
		   (abstract-eval1 (application-argument e)
				   (restrict-environment
				    (environment-binding-values b)
				    (application-argument-indices e))))))))
	;; With the above union the old value will always be a subset of the
	;; new value by a precise calculation but might not be given that the
	;; subset calculation is imprecise. Need to document example where this
	;; occurs.
	(unless (abstract-value-subset? v (environment-binding-value b))
	 (set-environment-binding-value! b v)
	 (for-each enqueue! (expression-parents e)))))
      (expression-environment-bindings e)))))
  ((letrec-expression? e)
   (for-each
    (lambda (b)
     ;; This corresponds to call C to c:widen in generate-expression.
     (let ((v (widen-abstract-value
	       ;; See the above note.
	       (abstract-value-union
		(environment-binding-value b)
		(abstract-eval1 (letrec-expression-body e)
				(letrec-nested-environment
				 (environment-binding-values b) e))))))
      ;; See the above note.
      (unless (abstract-value-subset? v (environment-binding-value b))
       (set-environment-binding-value! b v)
       (for-each enqueue! (expression-parents e)))))
    (expression-environment-bindings e)))
  ((cons-expression? e)
   (for-each
    (lambda (b)
     (let ((v1 (abstract-eval1
		(cons-expression-car e)
		(restrict-environment (environment-binding-values b)
				      (cons-expression-car-indices e))))
	   (v2 (abstract-eval1
		(cons-expression-cdr e)
		(restrict-environment (environment-binding-values b)
				      (cons-expression-cdr-indices e)))))
      (cond
       ((and
	 (every-value-tags
	  (lambda (tags1) (prefix-tags? (cons-expression-tags e) tags1)) v1)
	 (every-value-tags
	  (lambda (tags2) (prefix-tags? (cons-expression-tags e) tags2)) v2))
	;; This can widen when the tagged pair created violates the syntactic
	;; constraints (presumably tagged pair depth limit or some width
	;; limit). This corresponds to call D to c:widen in
	;; generate-expression.
	(let ((v (widen-abstract-value
		  ;; See the above note.
		  (abstract-value-union
		   (environment-binding-value b)
		   (new-tagged-pair (cons-expression-tags e) v1 v2)))))
	 ;; See the above note.
	 (unless (abstract-value-subset? v (environment-binding-value b))
	  (set-environment-binding-value! b v)
	  (for-each enqueue! (expression-parents e)))))
       ((and
	 (some-value-tags
	  (lambda (tags1) (prefix-tags? (cons-expression-tags e) tags1)) v1)
	 (some-value-tags
	  (lambda (tags2) (prefix-tags? (cons-expression-tags e) tags2)) v2))
	(unless (every-value-tags
		 (lambda (tags1) (prefix-tags? (cons-expression-tags e) tags1))
		 v1)
	 (compile-time-warning
	  (format #f
		  "CAR argument might have wrong type for target with tags ~s"
		  (cons-expression-tags e))
	  v1))
	(unless (every-value-tags
		 (lambda (tags2) (prefix-tags? (cons-expression-tags e) tags2))
		 v2)
	 (compile-time-warning
	  (format #f
		  "CDR argument might have wrong type for target with tags ~s"
		  (cons-expression-tags e))
	  v2))
	;; This can widen when the tagged pair created violates the syntactic
	;; constraints (presumably tagged pair depth limit or some width
	;; limit). This corresponds to call D to c:widen in
	;; generate-expression.
	(let ((v (widen-abstract-value
		  ;; See the above note.
		  (abstract-value-union
		   (environment-binding-value b)
		   (new-tagged-pair (cons-expression-tags e) v1 v2)))))
	 ;; See the above note.
	 (unless (abstract-value-subset? v (environment-binding-value b))
	  (set-environment-binding-value! b v)
	  (for-each enqueue! (expression-parents e)))))
       (else
	(unless (every-value-tags
		 (lambda (tags1) (prefix-tags? (cons-expression-tags e) tags1))
		 v1)
	 (compile-time-warning
	  (format #f
		  "CAR argument might have wrong type for target with tags ~s"
		  (cons-expression-tags e))
	  v1))
	(unless (every-value-tags
		 (lambda (tags2) (prefix-tags? (cons-expression-tags e) tags2))
		 v2)
	 (compile-time-warning
	  (format #f
		  "CDR argument might have wrong type for target with tags ~s"
		  (cons-expression-tags e))
	  v2))))))
    (expression-environment-bindings e)))
  (else (internal-error))))

(define (abstract-apply-closure! e u1 v2)
 (assert (not (union? u1)))
 (for-each (lambda (alist)
	    (let ((e1 (closure-body u1)))
	     ;; See the note in abstract-eval-prime!
	     (unless (memp expression-eqv? e (expression-parents e1))
	      (set-expression-parents! e1 (cons e (expression-parents e1)))
	      (enqueue! e))
	     (abstract-eval-prime! e1 (construct-environment u1 alist))))
	   (abstract-destructure (closure-parameter u1) v2)))

(define (abstract-apply-prime! e v1 v2)
 (unless (empty-abstract-value? v2)
  (for-each
   (lambda (u1)
    (cond
     ((primitive-procedure? u1)
      ;; needs work: Should put this into slots of the primitive procedures.
      (when (eq? (primitive-procedure-name u1) 'if-procedure)
       (for-each
	(lambda (u)
	 (if (vlad-pair? u)
	     (for-each
	      (lambda (u1)
	       (for-each
		(lambda (u23)
		 (if (vlad-pair? u23)
		     (for-each
		      (lambda (u2)
		       (for-each
			(lambda (u3)
			 ;; When v3 and/or v2 is not a procedure the warning is
			 ;; issued by abstract-apply. If it is a primitive
			 ;; procedure we don't have to do anything here. In
			 ;; practise, it will always be a nonrecursive closure
			 ;; unless the user calls if-procedure outside the
			 ;; context of the if macro.
			 (if (vlad-false? u1)
			     (when (closure? u3)
			      (abstract-apply-closure! e u3 (vlad-empty-list)))
			     (when (closure? u2)
			      (abstract-apply-closure!
			       e u2 (vlad-empty-list)))))
			(union-members (vlad-cdr u23))))
		      (union-members (vlad-car u23)))
		     (compile-time-warning
		      "Argument to if-procedure might be invalid" u)))
		(union-members (vlad-cdr u))))
	      (union-members (vlad-car u)))
	     (compile-time-warning
	      "Argument to if-procedure might be invalid" u)))
	(union-members v2))))
     ((closure? u1)
      (cond ((every-value-tags
	      (lambda (tags2) (prefix-tags? (value-tags u1) tags2)) v2)
	     (abstract-apply-closure! e u1 v2))
	    ((some-value-tags
	      (lambda (tags2) (prefix-tags? (value-tags u1) tags2)) v2)
	     (compile-time-warning
	      "Argument might have wrong type for target" u1 v2)
	     (abstract-apply-closure! e u1 v2))
	    (else (compile-time-warning
		   "Argument might have wrong type for target" u1 v2))))
     (else (compile-time-warning "Target might not be a procedure" u1))))
   (union-members v1))))

(define (abstract-eval-prime! e vs)
 ;; Can't give an error if entry already exists since we call this
 ;; indiscriminantly in abstract-apply-prime!.
 ;; The abstract values in vs might violate the syntactic constraints. We adopt
 ;; the constraint that all abstract values in all environment bindings satisfy
 ;; the syntactic constraints. We widen here so that we compare widened values
 ;; to widened values. We also take care below to widen appropriately but delay
 ;; widening as long as possible.
 (let loop ((e e) (vs vs))
  (unless (let ((vs (map widen-abstract-value vs)))
	   (some (lambda (b)
		  (abstract-environment=? vs (environment-binding-values b)))
		 (expression-environment-bindings e)))
   (cond
    ((constant-expression? e)
     (set-expression-environment-bindings!
      e
      (cons (make-environment-binding
	     (map widen-abstract-value vs)
	     ;; This can widen when the constant expression value violates the
	     ;; syntactic constraints (presumably tagged pair depth limit).
	     ;; This corresponds to call E to c:widen in generate-expression.
	     (widen-abstract-value
	      (concrete-value->abstract-value (constant-expression-value e))))
	    (expression-environment-bindings e)))
     (for-each enqueue! (expression-parents e)))
    ((variable-access-expression? e)
     (set-expression-environment-bindings!
      e
      (let ((vs (map widen-abstract-value vs)))
       ;; There does not need to be a corresponding call F to c:widen in
       ;; generate-expression.
       (cons (make-environment-binding vs (first vs))
	     (expression-environment-bindings e))))
     (for-each enqueue! (expression-parents e)))
    ((lambda-expression? e)
     (set-expression-environment-bindings!
      e
      (cons (make-environment-binding
	     (map widen-abstract-value vs)
	     ;; This can widen when the closure created violates the syntactic
	     ;; constraints (presumably closure depth limit or backpropagator
	     ;; depth limit). Note that we don't widen vs before creating the
	     ;; closure. This corresponds to call G to c:widen in
	     ;; generate-expression.
	     (widen-abstract-value (new-nonrecursive-closure vs e)))
	    (expression-environment-bindings e)))
     (for-each enqueue! (expression-parents e)))
    ((application? e)
     (cond
      ((lambda-expression? (application-callee e))
       ;; This handling of LET is an optimization.
       (set-expression-environment-bindings!
	e
	(cons (make-environment-binding
	       (map widen-abstract-value vs) (empty-abstract-value))
	      (expression-environment-bindings e)))
       ;; Can't give an error if parent already in list since could have done
       ;; this for a different context.
       (unless (memp expression-eqv?
		     e
		     (expression-parents (application-argument e)))
	(set-expression-parents!
	 (application-argument e)
	 (cons e (expression-parents (application-argument e)))))
       (loop (application-argument e)
	     (restrict-environment vs (application-argument-indices e)))
       (enqueue! e))
      (else
       (set-expression-environment-bindings!
	e
	(cons (make-environment-binding
	       (map widen-abstract-value vs) (empty-abstract-value))
	      (expression-environment-bindings e)))
       ;; Can't give an error if parent already in list since could have done
       ;; this for a different context.
       (unless (memp
		expression-eqv? e (expression-parents (application-callee e)))
	(set-expression-parents!
	 (application-callee e)
	 (cons e (expression-parents (application-callee e)))))
       (unless (memp expression-eqv?
		     e
		     (expression-parents (application-argument e)))
	(set-expression-parents!
	 (application-argument e)
	 (cons e (expression-parents (application-argument e)))))
       (loop (application-callee e)
	     (restrict-environment vs (application-callee-indices e)))
       (loop (application-argument e)
	     (restrict-environment vs (application-argument-indices e)))
       (enqueue! e))))
    ((letrec-expression? e)
     (set-expression-environment-bindings!
      e
      (cons (make-environment-binding
	     (map widen-abstract-value vs) (empty-abstract-value))
	    (expression-environment-bindings e)))
     ;; Ditto.
     (unless (memp expression-eqv?
		   e
		   (expression-parents (letrec-expression-body e)))
      (set-expression-parents!
       (letrec-expression-body e)
       (cons e (expression-parents (letrec-expression-body e)))))
     (loop (letrec-expression-body e)
	   ;; Note that we don't widen vs before passing to
	   ;; letrec-nested-environment and rely on abstract-eval-prime! to
	   ;; enforce the syntactic constraints.
	   (letrec-nested-environment vs e))
     (enqueue! e))
    ((cons-expression? e)
     (set-expression-environment-bindings!
      e
      (cons (make-environment-binding
	     (map widen-abstract-value vs) (empty-abstract-value))
	    (expression-environment-bindings e)))
     ;; Ditto.
     (unless (memp
	      expression-eqv? e (expression-parents (cons-expression-car e)))
      (set-expression-parents!
       (cons-expression-car e)
       (cons e (expression-parents (cons-expression-car e)))))
     (unless (memp
	      expression-eqv? e (expression-parents (cons-expression-cdr e)))
      (set-expression-parents!
       (cons-expression-cdr e)
       (cons e (expression-parents (cons-expression-cdr e)))))
     (loop (cons-expression-car e)
	   (restrict-environment vs (cons-expression-car-indices e)))
     (loop (cons-expression-cdr e)
	   (restrict-environment vs (cons-expression-cdr-indices e)))
     (enqueue! e))
    (else (internal-error))))))

(define (deep-empty-abstract-value? v)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  26
  (let outer ((v v) (vs '()) (k (lambda (r? vs) r?)))
   (cond ((real? v) (k #f vs))
	 ((memq v vs) (k #f vs))
	 ((union? v)
	  (if (null? (union-members v))
	      (k #t vs)
	      (let inner ((us (union-members v)) (vs (cons v vs)))
	       (if (null? us)
		   (k #f vs)
		   (outer (first us)
			  vs
			  (lambda (r? vs)
			   (if r? (k #t vs) (inner (rest us) vs))))))))
	 ((scalar-value? v) (k #f vs))
	 (else (let inner ((vs1 (aggregate-value-values v)) (vs (cons v vs)))
		(if (null? vs1)
		    (k #f vs)
		    (outer (first vs1)
			   vs
			   (lambda (r? vs)
			    (if r? (k #t vs) (inner (rest vs1) vs)))))))))))

(define (value-contains-unfilled? v)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  27
  (let outer ((v v) (vs '()) (k (lambda (r? vs) r?)))
   (cond ((real? v) (k #f vs))
	 ((memq v vs) (k #f vs))
	 ((union? v)
	  (if (eq? (union-values v) 'unfilled)
	      (k #t vs)
	      (let inner ((us (union-members v)) (vs (cons v vs)))
	       (if (null? us)
		   (k #f vs)
		   (outer (first us)
			  vs
			  (lambda (r? vs)
			   (if r? (k #t vs) (inner (rest us) vs))))))))
	 ((scalar-value? v) (k #f vs))
	 (else
	  (if (or (and (nonrecursive-closure? v)
		       (eq? (nonrecursive-closure-values v) 'unfilled))
		  (and (recursive-closure? v)
		       (eq? (recursive-closure-values v) 'unfilled))
		  (and (perturbation-tagged-value? v)
		       (eq? (perturbation-tagged-value-primal v) 'unfilled))
		  (and (bundle? v)
		       (or (eq? (bundle-primal v) 'unfilled)
			   (eq? (bundle-tangent v) 'unfilled)))
		  (and (sensitivity-tagged-value? v)
		       (eq? (sensitivity-tagged-value-primal v) 'unfilled))
		  (and (reverse-tagged-value? v)
		       (eq? (reverse-tagged-value-primal v) 'unfilled))
		  (and (tagged-pair? v)
		       (or (eq? (tagged-pair-car v) 'unfilled)
			   (eq? (tagged-pair-cdr v) 'unfilled))))
	      (k #t vs)
	      (let inner ((vs1 (aggregate-value-values v)) (vs (cons v vs)))
	       (if (null? vs1)
		   (k #f vs)
		   (outer (first vs1)
			  vs
			  (lambda (r? vs)
			   (if r? (k #t vs) (inner (rest vs1) vs))))))))))))

(define (value-contains-union? v)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  28
  (let outer ((v v) (vs '()) (k (lambda (r? vs) r?)))
   (cond ((real? v) (k #f vs))
	 ((memq v vs) (k #f vs))
	 ((union? v)
	  (if (>= (length (union-members v)) 2)
	      (k #t vs)
	      (let inner ((us (union-members v)) (vs (cons v vs)))
	       (if (null? us)
		   (k #f vs)
		   (outer (first us)
			  vs
			  (lambda (r? vs)
			   (if r? (k #t vs) (inner (rest us) vs))))))))
	 ((scalar-value? v) (k #f vs))
	 (else (let inner ((vs1 (aggregate-value-values v)) (vs (cons v vs)))
		(if (null? vs1)
		    (k #f vs)
		    (outer (first vs1)
			   vs
			   (lambda (r? vs)
			    (if r? (k #t vs) (inner (rest vs1) vs)))))))))))

(define (unions-in-abstract-value v)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  29
  (let outer ((v v) (vs '()) (n 0) (k (lambda (n vs) n)))
   (cond ((real? v) (k n vs))
	 ((memq v vs) (k n vs))
	 ((union? v)
	  (let inner ((us (union-members v))
		      (vs (cons v vs))
		      (n (+ n (if (>= (length (union-members v)) 2) 1 0))))
	   (if (null? us)
	       (k n vs)
	       (outer (first us)
		      vs
		      n
		      (lambda (n vs) (inner (rest us) vs n))))))
	 ((scalar-value? v) (k n vs))
	 (else (let inner ((vs1 (aggregate-value-values v))
			   (vs (cons v vs))
			   (n n))
		(if (null? vs1)
		    (k n vs)
		    (outer (first vs1)
			   vs
			   n
			   (lambda (n vs) (inner (rest vs1) vs n))))))))))

(define (type-in-abstract-value type? v)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  30
  (let outer ((v v) (vs '()) (n '()) (k (lambda (n vs) n)))
   (cond ((memq v vs) (k n vs))
	 ((union? v)
	  (let inner ((us (union-members v))
		      (vs (cons v vs))
		      (n (if (type? v) (cons v n) n)))
	   (if (null? us)
	       (k n vs)
	       (outer (first us)
		      vs
		      n
		      (lambda (n vs) (inner (rest us) vs n))))))
	 ((scalar-value? v) (k (if (type? v) (cons v n) n) vs))
	 (else (let inner ((vs1 (aggregate-value-values v))
			   (vs (cons v vs))
			   (n (if (type? v) (cons v n) n)))
		(if (null? vs1)
		    (k n vs)
		    (outer (first vs1)
			   vs
			   n
			   (lambda (n vs) (inner (rest vs1) vs n))))))))))

(define (concrete-reals-in-abstract-value v)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  31
  (let outer ((v v) (vs '()) (n '()) (k (lambda (n vs) n)))
   (cond
    ((real? v) (k (if (memv v n) n (cons v n)) vs))
    ((memq v vs) (k n vs))
    ((union? v)
     (let inner ((us (union-members v)) (vs (cons v vs)) (n n))
      (if (null? us)
	  (k n vs)
	  (outer (first us)
		 vs
		 n
		 (lambda (n vs) (inner (rest us) vs n))))))
    ((scalar-value? v) (k n vs))
    (else (let inner ((vs1 (aggregate-value-values v)) (vs (cons v vs)) (n n))
	   (if (null? vs1)
	       (k n vs)
	       (outer (first vs1)
		      vs
		      n
		      (lambda (n vs) (inner (rest vs1) vs n))))))))))

(define (value-size v)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  32
  (let outer ((v v) (vs '()) (n 0) (k (lambda (n vs) n)))
   (cond
    ((memq v vs) (k n vs))
    ;; We intentionally omit the special case for real here but not elsewhere.
    ((union? v)
     (let inner ((us (union-members v)) (vs (cons v vs)) (n (+ n 1)))
      (if (null? us)
	  (k n vs)
	  (outer (first us) vs n (lambda (n vs) (inner (rest us) vs n))))))
    ;; We intentionally cons here but not elsewhere.
    ((scalar-value? v) (k (+ n 1) (cons v vs)))
    (else (let inner ((vs1 (aggregate-value-values v))
		      (vs (cons v vs))
		      (n (+ n 1)))
	   (if (null? vs1)
	       (k n vs)
	       (outer (first vs1)
		      vs
		      n
		      (lambda (n vs) (inner (rest vs1) vs n))))))))))

(define (value-max-width v)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  33
  (let outer ((v v) (vs '()) (n 0) (k (lambda (n vs) n)))
   (cond
    ((real? v) (k (max n 1) vs))
    ((memq v vs) (k n vs))
    ((union? v)
     (let inner ((us (union-members v))
		 (vs (cons v vs))
		 (n (max n (length (union-members v)))))
      (if (null? us)
	  (k n vs)
	  (outer (first us) vs n (lambda (n vs) (inner (rest us) vs n))))))
    ((scalar-value? v) (k (max n 1) vs))
    (else (let inner ((vs1 (aggregate-value-values v))
		      (vs (cons v vs))
		      (n (max n 1)))
	   (if (null? vs1)
	       (k n vs)
	       (outer (first vs1)
		      vs
		      n
		      (lambda (n vs) (inner (rest vs1) vs n))))))))))

(define (analysis-size)
 (map-reduce +
	     0
	     (lambda (e) (length (expression-environment-bindings e)))
	     *expressions*))

(define (max-flow-size)
 (map-reduce max
	     minus-infinity
	     (lambda (e) (length (expression-environment-bindings e)))
	     *expressions*))

(define (analysis-contains-union?)
 (some (lambda (e)
	(some (lambda (b)
	       (or (some value-contains-union?
			 (environment-binding-values b))
		   (value-contains-union? (environment-binding-value b))))
	      (expression-environment-bindings e)))
       *expressions*))

(define (unions-in-analysis)
 (map-reduce
  +
  0
  (lambda (e)
   (map-reduce
    +
    0
    (lambda (b)
     (+ (map-reduce
	 + 0 unions-in-abstract-value (environment-binding-values b))
	(unions-in-abstract-value (environment-binding-value b))))
    (expression-environment-bindings e)))
  *expressions*))

(define (concrete-reals-in-analysis)
 (map-reduce
  unionv
  '()
  (lambda (e)
   (map-reduce
    unionv
    '()
    (lambda (b)
     (unionv
      (map-reduce unionv
		  '()
		  concrete-reals-in-abstract-value
		  (environment-binding-values b))
      (concrete-reals-in-abstract-value (environment-binding-value b))))
    (expression-environment-bindings e)))
  *expressions*))

(define (bottoms-in-analysis)
 (map-reduce
  +
  0
  (lambda (e)
   (count-if (lambda (b) (empty-abstract-value? (environment-binding-value b)))
	     (expression-environment-bindings e)))
  *expressions*))

(define (analysis-max-size)
 (map-reduce
  max
  0
  (lambda (e)
   (map-reduce
    max
    0
    (lambda (b)
     (max (map-reduce max 0 value-size (environment-binding-values b))
	  (value-size (environment-binding-value b))))
    (expression-environment-bindings e)))
  *expressions*))

(define (analysis-max-width)
 (map-reduce
  max
  0
  (lambda (e)
   (map-reduce
    max
    0
    (lambda (b)
     (max (map-reduce max 0 value-max-width (environment-binding-values b))
	  (value-max-width (environment-binding-value b))))
    (expression-environment-bindings e)))
  *expressions*))

(define (check-canonize-cache! v)
 (cond ((union? v) (assert (eq? (union-canonize-cache v) v)))
       ((scalar-value? v) #f)
       ((nonrecursive-closure? v)
	(assert (eq? (nonrecursive-closure-canonize-cache v) v)))
       ((recursive-closure? v)
	(assert (eq? (recursive-closure-canonize-cache v) v)))
       ((perturbation-tagged-value? v)
	(assert (eq? (perturbation-tagged-value-canonize-cache v) v)))
       ((bundle? v) (assert (eq? (bundle-canonize-cache v) v)))
       ((sensitivity-tagged-value? v)
	(assert (eq? (sensitivity-tagged-value-canonize-cache v) v)))
       ((reverse-tagged-value? v)
	(assert (eq? (reverse-tagged-value-canonize-cache v) v)))
       ((tagged-pair? v) (assert (eq? (tagged-pair-canonize-cache v) v)))
       (else (internal-error))))

(define (check-intern-cache! v)
 (cond ((union? v) (assert (eq? (union-intern-cache v) v)))
       ((scalar-value? v) #f)
       ((nonrecursive-closure? v)
	(assert (eq? (nonrecursive-closure-intern-cache v) v)))
       ((recursive-closure? v)
	(assert (eq? (recursive-closure-intern-cache v) v)))
       ((perturbation-tagged-value? v)
	(assert (eq? (perturbation-tagged-value-intern-cache v) v)))
       ((bundle? v) (assert (eq? (bundle-intern-cache v) v)))
       ((sensitivity-tagged-value? v)
	(assert (eq? (sensitivity-tagged-value-intern-cache v) v)))
       ((reverse-tagged-value? v)
	(assert (eq? (reverse-tagged-value-intern-cache v) v)))
       ((tagged-pair? v) (assert (eq? (tagged-pair-intern-cache v) v)))
       (else (internal-error))))

(define (check-filled! v)
 (cond ((union? v) (assert (not (eq? (union-values v) 'unfilled))))
       ((scalar-value? v) #f)
       ((nonrecursive-closure? v)
	(assert (not (eq? (nonrecursive-closure-values v) 'unfilled))))
       ((recursive-closure? v)
	(assert (not (eq? (recursive-closure-values v) 'unfilled))))
       ((perturbation-tagged-value? v)
	(assert (not (eq? (perturbation-tagged-value-primal v) 'unfilled))))
       ((bundle? v)
	(assert (and (not (eq? (bundle-primal v) 'unfilled))
		     (not (eq? (bundle-tangent v) 'unfilled)))))
       ((sensitivity-tagged-value? v)
	(assert (not (eq? (sensitivity-tagged-value-primal v) 'unfilled))))
       ((reverse-tagged-value? v)
	(assert (not (eq? (reverse-tagged-value-primal v) 'unfilled))))
       ((tagged-pair? v)
	(assert (and (not (eq? (tagged-pair-car v) 'unfilled))
		     (not (eq? (tagged-pair-cdr v) 'unfilled)))))
       (else (internal-error))))

(define (check-interned! v)
 (cond ((union? v) (assert (memq v *unions*)))
       ((scalar-value? v) #f)
       ((nonrecursive-closure? v)
	(assert (memq v
		      (lambda-expression-nonrecursive-closures
		       (nonrecursive-closure-lambda-expression v)))))
       ((recursive-closure? v)
	(assert
	 (memq v
	       (lambda-expression-recursive-closures
		(vector-ref (recursive-closure-lambda-expressions v) 0)))))
       ((perturbation-tagged-value? v)
	(assert (memq v *perturbation-tagged-values*)))
       ((bundle? v) (assert (memq v *bundles*)))
       ((sensitivity-tagged-value? v)
	(assert (memq v *sensitivity-tagged-values*)))
       ((reverse-tagged-value? v) (assert (memq v *reverse-tagged-values*)))
       ((tagged-pair? v) (assert (memq v *tagged-pairs*)))
       (else (internal-error))))

(define (check-no-nested-or-singleton-unions! v)
 (when (union? v)
  (assert (and (not (some union? (get-union-values v)))
	       (not (= (length (get-union-values v)) 1))))))

(define (check-no-empty-slots! v)
 (unless (or (union? v) (scalar-value? v))
  (for-each (lambda (v) (assert (not (empty-abstract-value? v))))
	    (aggregate-value-values v))))

(define (check-slots-interned! v)
 (cond ((union? v) (for-each check-interned! (get-union-values v)))
       ((scalar-value? v) #f)
       (else (for-each check-interned! (aggregate-value-values v)))))

(define (check-no-subsumptions! v)
 (when (union? v)
  (for-each-indexed
   (lambda (u1 i1)
    (for-each-indexed
     (lambda (u2 i2)
      (assert (or (= i1 i2) (not (abstract-value-subset? u1 u2)))))
     (union-members v)))
   (union-members v))))

(define (check-abstract-value! v)
 (check-canonize-cache! v)
 (check-intern-cache! v)
 (check-filled! v)
 (check-no-nested-or-singleton-unions! v)
 (check-no-empty-slots! v)
 (check-slots-interned! v)
 (check-no-subsumptions! v))

(define (for-each-interned-abstract-value! f)
 (for-each f *unions*)
 (for-each (lambda (e)
	    (when (lambda-expression? e)
	     (for-each f (lambda-expression-nonrecursive-closures e))
	     (for-each f (lambda-expression-recursive-closures e))))
	   *expressions*)
 (for-each f *perturbation-tagged-values*)
 (for-each f *bundles*)
 (for-each f *sensitivity-tagged-values*)
 (for-each f *reverse-tagged-values*)
 (for-each f *tagged-pairs*))

(define (check-no-duplicate-interned-abstract-values!)
 (define (check-no-duplicate-interned-abstract-values! vs)
  (for-each-indexed
   (lambda (v1 i1)
    (for-each-indexed
     (lambda (v2 i2)
      (assert (or (= i1 i2) (not (deep-abstract-value=? v1 v2)))))
     vs))
   vs))
 (check-no-duplicate-interned-abstract-values! *unions*)
 (for-each (lambda (e)
	    (when (lambda-expression? e)
	     (check-no-duplicate-interned-abstract-values!
	      (lambda-expression-nonrecursive-closures e))
	     (check-no-duplicate-interned-abstract-values!
	      (lambda-expression-recursive-closures e))))
	   *expressions*)
 (check-no-duplicate-interned-abstract-values! *perturbation-tagged-values*)
 (check-no-duplicate-interned-abstract-values! *bundles*)
 (check-no-duplicate-interned-abstract-values! *sensitivity-tagged-values*)
 (check-no-duplicate-interned-abstract-values! *reverse-tagged-values*)
 (check-no-duplicate-interned-abstract-values! *tagged-pairs*))

(define (walk-abstract-value! f v)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  34
  (let outer ((v v) (vs '()) (k (lambda (vs) #f)))
   (f v)
   (cond ((real? v) (k vs))
	 ((memq v vs) (k vs))
	 ((union? v)
	  (let inner ((us (union-members v)) (vs (cons v vs)))
	   (if (null? us)
	       (k vs)
	       (outer (first us) vs (lambda (vs) (inner (rest us) vs))))))
	 ((scalar-value? v) (k vs))
	 (else (let inner ((vs1 (aggregate-value-values v)) (vs (cons v vs)))
		(if (null? vs1)
		    (k vs)
		    (outer (first vs1)
			   vs
			   (lambda (vs) (inner (rest vs1) vs))))))))))

(define (check-analysis!)
 (for-each
  (lambda (e)
   (for-each (lambda (b)
	      (for-each check-interned! (environment-binding-values b))
	      (check-interned! (environment-binding-value b)))
	     (expression-environment-bindings e)))
  *expressions*)
 (for-each-interned-abstract-value! check-abstract-value!)
 (check-no-duplicate-interned-abstract-values!))

(define (verbosity)
 (format #t
	 "expressions: ~s, |analysis|=~s, max flow size: ~s, |queue|=~s~%unions: ~s, bottoms: ~s, max size: ~s, max width: ~s~%concrete reals: ~s~%"
	 (count-if
	  (lambda (e) (not (null? (expression-environment-bindings e))))
	  *expressions*)
	 (analysis-size)
	 (max-flow-size)
	 (length *queue*)
	 (unions-in-analysis)
	 (bottoms-in-analysis)
	 (analysis-max-size)
	 (analysis-max-width)
	 (concrete-reals-in-analysis)))

(define (flow-analysis! e bs)
 (with-abstract
  (lambda ()
   (abstract-eval-prime!
    e
    (map
     (lambda (x)
      (value-binding-value
       (find-if (lambda (b) (variable=? x (value-binding-variable b))) bs)))
     (free-variables e)))
   (let loop ((i 0))
    (when (and
	   *verbose* (not (zero? *verbose*)) (zero? (remainder i *verbose*)))
     (verbosity))
    (unless (null? *queue*)
     (let ((e (first *queue*)))
      (set! *queue* (rest *queue*))
      (assert (expression-enqueue? e))
      (set-expression-enqueue?! e #f)
      (abstract-eval! e))
     (loop (+ i 1))))
   (check-analysis!)
   (when *verbose* (verbosity)))))

;;; Symbolic Evaluator

(define (inline-zero? v)
 (cond
  ((union? v) (union-inline-zero? v))
  ((scalar-value? v) #t)
  ((nonrecursive-closure? v) (nonrecursive-closure-inline-zero? v))
  ((recursive-closure? v) (recursive-closure-inline-zero? v))
  ((perturbation-tagged-value? v) (perturbation-tagged-value-inline-zero? v))
  ((bundle? v) (bundle-inline-zero? v))
  ((sensitivity-tagged-value? v) (sensitivity-tagged-value-inline-zero? v))
  ((reverse-tagged-value? v) (reverse-tagged-value-inline-zero? v))
  ((tagged-pair? v) (tagged-pair-inline-zero? v))
  (else (internal-error))))

(define (inline-perturb? v)
 (cond
  ((union? v) (union-inline-perturb? v))
  ((scalar-value? v) #t)
  ((nonrecursive-closure? v) (nonrecursive-closure-inline-perturb? v))
  ((recursive-closure? v) (recursive-closure-inline-perturb? v))
  ((perturbation-tagged-value? v)
   (perturbation-tagged-value-inline-perturb? v))
  ((bundle? v) (bundle-inline-perturb? v))
  ((sensitivity-tagged-value? v) (sensitivity-tagged-value-inline-perturb? v))
  ((reverse-tagged-value? v) (reverse-tagged-value-inline-perturb? v))
  ((tagged-pair? v) (tagged-pair-inline-perturb? v))
  (else (internal-error))))

(define (inline-unperturb? v)
 (cond ((union? v) (union-inline-unperturb? v))
       ((scalar-value? v) #t)
       ((nonrecursive-closure? v) (nonrecursive-closure-inline-unperturb? v))
       ((recursive-closure? v) (recursive-closure-inline-unperturb? v))
       ((perturbation-tagged-value? v)
	(perturbation-tagged-value-inline-unperturb? v))
       ((bundle? v) (bundle-inline-unperturb? v))
       ((sensitivity-tagged-value? v)
	(sensitivity-tagged-value-inline-unperturb? v))
       ((reverse-tagged-value? v) (reverse-tagged-value-inline-unperturb? v))
       ((tagged-pair? v) (tagged-pair-inline-unperturb? v))
       (else (internal-error))))

(define (inline-primal? v)
 (cond
  ((union? v) (union-inline-primal? v))
  ((scalar-value? v) #t)
  ((nonrecursive-closure? v) (nonrecursive-closure-inline-primal? v))
  ((recursive-closure? v) (recursive-closure-inline-primal? v))
  ((perturbation-tagged-value? v) (perturbation-tagged-value-inline-primal? v))
  ((bundle? v) (bundle-inline-primal? v))
  ((sensitivity-tagged-value? v) (sensitivity-tagged-value-inline-primal? v))
  ((reverse-tagged-value? v) (reverse-tagged-value-inline-primal? v))
  ((tagged-pair? v) (tagged-pair-inline-primal? v))
  (else (internal-error))))

(define (inline-tangent? v)
 (cond
  ((union? v) (union-inline-tangent? v))
  ((scalar-value? v) #t)
  ((nonrecursive-closure? v) (nonrecursive-closure-inline-tangent? v))
  ((recursive-closure? v) (recursive-closure-inline-tangent? v))
  ((perturbation-tagged-value? v)
   (perturbation-tagged-value-inline-tangent? v))
  ((bundle? v) (bundle-inline-tangent? v))
  ((sensitivity-tagged-value? v) (sensitivity-tagged-value-inline-tangent? v))
  ((reverse-tagged-value? v) (reverse-tagged-value-inline-tangent? v))
  ((tagged-pair? v) (tagged-pair-inline-tangent? v))
  (else (internal-error))))

(define (inline-bundle? v)
 (cond ((union? v) (union-inline-bundle? v))
       ((scalar-value? v) #t)
       ((nonrecursive-closure? v) #t)
       ((recursive-closure? v) #t)
       ((perturbation-tagged-value? v) #t)
       ((bundle? v) #t)
       ((sensitivity-tagged-value? v) #t)
       ((reverse-tagged-value? v) #t)
       ((tagged-pair? v) (tagged-pair-inline-bundle? v))
       (else (internal-error))))

(define (inline-sensitize? v)
 (cond ((union? v) (union-inline-sensitize? v))
       ((scalar-value? v) #t)
       ((nonrecursive-closure? v) (nonrecursive-closure-inline-sensitize? v))
       ((recursive-closure? v) (recursive-closure-inline-sensitize? v))
       ((perturbation-tagged-value? v)
	(perturbation-tagged-value-inline-sensitize? v))
       ((bundle? v) (bundle-inline-sensitize? v))
       ((sensitivity-tagged-value? v)
	(sensitivity-tagged-value-inline-sensitize? v))
       ((reverse-tagged-value? v) (reverse-tagged-value-inline-sensitize? v))
       ((tagged-pair? v) (tagged-pair-inline-sensitize? v))
       (else (internal-error))))

(define (inline-unsensitize? v)
 (cond ((union? v) (union-inline-unsensitize? v))
       ((scalar-value? v) #t)
       ((nonrecursive-closure? v) (nonrecursive-closure-inline-unsensitize? v))
       ((recursive-closure? v) (recursive-closure-inline-unsensitize? v))
       ((perturbation-tagged-value? v)
	(perturbation-tagged-value-inline-unsensitize? v))
       ((bundle? v) (bundle-inline-unsensitize? v))
       ((sensitivity-tagged-value? v)
	(sensitivity-tagged-value-inline-unsensitize? v))
       ((reverse-tagged-value? v) (reverse-tagged-value-inline-unsensitize? v))
       ((tagged-pair? v) (tagged-pair-inline-unsensitize? v))
       (else (internal-error))))

(define (inline-plus? v)
 (cond ((union? v) (union-inline-plus? v))
       ((scalar-value? v) #t)
       ((nonrecursive-closure? v) #t)
       ((recursive-closure? v) #t)
       ((perturbation-tagged-value? v) #t)
       ((bundle? v) #t)
       ((sensitivity-tagged-value? v) #t)
       ((reverse-tagged-value? v) #t)
       ((tagged-pair? v) (tagged-pair-inline-plus? v))
       (else (internal-error))))

(define (inline-*j? v)
 (cond
  ((union? v) (union-inline-*j? v))
  ((scalar-value? v) #t)
  ((nonrecursive-closure? v) (nonrecursive-closure-inline-*j? v))
  ((recursive-closure? v) (recursive-closure-inline-*j? v))
  ((perturbation-tagged-value? v) (perturbation-tagged-value-inline-*j? v))
  ((bundle? v) (bundle-inline-*j? v))
  ((sensitivity-tagged-value? v) (sensitivity-tagged-value-inline-*j? v))
  ((reverse-tagged-value? v) (reverse-tagged-value-inline-*j? v))
  ((tagged-pair? v) (tagged-pair-inline-*j? v))
  (else (internal-error))))

(define (inline-*j-inverse? v)
 (cond ((union? v) (union-inline-*j-inverse? v))
       ((scalar-value? v) #t)
       ((nonrecursive-closure? v) (nonrecursive-closure-inline-*j-inverse? v))
       ((recursive-closure? v) (recursive-closure-inline-*j-inverse? v))
       ((perturbation-tagged-value? v)
	(perturbation-tagged-value-inline-*j-inverse? v))
       ((bundle? v) (bundle-inline-*j-inverse? v))
       ((sensitivity-tagged-value? v)
	(sensitivity-tagged-value-inline-*j-inverse? v))
       ((reverse-tagged-value? v) (reverse-tagged-value-inline-*j-inverse? v))
       ((tagged-pair? v) (tagged-pair-inline-*j-inverse? v))
       (else (internal-error))))

(define (unit? v)
 (or (name-unit? v)
     (call-unit? v)
     (panic-unit? v)
     (+-unit? v)
     (--unit? v)
     (*-unit? v)
     (/-unit? v)
     (sqrt-unit? v)
     (exp-unit? v)
     (log-unit? v)
     (sin-unit? v)
     (cos-unit? v)
     (atan-unit? v)
     (=-unit? v)
     (<-unit? v)
     (>-unit? v)
     (<=-unit? v)
     (>=-unit? v)
     (zero?-unit? v)
     (positive?-unit? v)
     (negative?-unit? v)
     (read-real-unit? v)
     (write-real-unit? v)))

(define (unit-abstract-value v)
 (cond ((name-unit? v) (name-unit-abstract-value v))
       ((call-unit? v) (call-unit-abstract-value v))
       ((panic-unit? v) (empty-abstract-value))
       ((+-unit? v) (abstract-real))
       ((--unit? v) (abstract-real))
       ((*-unit? v) (abstract-real))
       ((/-unit? v) (abstract-real))
       ((sqrt-unit? v) (abstract-real))
       ((exp-unit? v) (abstract-real))
       ((log-unit? v) (abstract-real))
       ((sin-unit? v) (abstract-real))
       ((cos-unit? v) (abstract-real))
       ((atan-unit? v) (abstract-real))
       ((=-unit? v) (abstract-boolean))
       ((<-unit? v) (abstract-boolean))
       ((>-unit? v) (abstract-boolean))
       ((<=-unit? v) (abstract-boolean))
       ((>=-unit? v) (abstract-boolean))
       ((zero?-unit? v) (abstract-boolean))
       ((positive?-unit? v) (abstract-boolean))
       ((negative?-unit? v) (abstract-boolean))
       ((read-real-unit? v) (abstract-real))
       ((write-real-unit? v) (abstract-real))
       (else (internal-error))))

(define (create-tagged-union tag vs)
 (let ((v (allocate-union vs)))
  (set-union-tag! v tag)
  v))

(define (flatten v)
 (cond ((unit? v) (list v))
       ((union? v)
	(append (flatten (union-tag v))
		(map-reduce append '() flatten (get-union-values v))))
       ((abstract-real? v) (internal-error))
       ((scalar-value? v) '())
       (else (map-reduce append '() flatten (aggregate-value-values v)))))

(define (unitify v s top? return?)
 (assert (not (unit? v)))
 (cond ((void? v) v)
       ((union? v)
	(if (and (not top?) (boxed? v))
	    (new-name-unit v s)
	    (create-tagged-union
	     (new-name-unit 'int (c:new-slot return? v s "v"))
	     (map (lambda (v1 s1)
		   (unitify v1 (c:new-slot return? v s s1) #f #f))
		  (get-union-values v)
		  (generate-slot-names v)))))
       ((abstract-real? v) (new-name-unit v s))
       (else (if (and (not top?) (boxed? v))
		 (new-name-unit v s)
		 (create-aggregate-value-with-new-values
		  v
		  (map (lambda (v1 s1)
			(unitify v1 (c:new-slot return? v s s1) #f #f))
		       (aggregate-value-values v)
		       (generate-slot-names v)))))))

(define (unroll v)
 (if (name-unit? v)
     (unitify (unit-abstract-value v) (name-unit-code v) #t #f)
     (let ((i (positionq v *units*)))
      (assert i)
      (unitify (unit-abstract-value v) (list "u" i) #t #f))))

(define (symbolic-destructure p v k)
 (let outer ((p p) (v v) (alist '()) (k k))
  (cond
   ;; This case comes first to avoid the dispatch.
   ((variable-access-expression? p)
    (k (cons (cons (variable-access-expression-variable p) v) alist)))
   ((and (unit? v)
	 (or (union? (unit-abstract-value v))
	     (closure? (unit-abstract-value v))
	     (tagged-pair? (unit-abstract-value v))))
    (outer p (unroll v) alist k))
   ((union? v)
    (create-tagged-union
     (union-tag v)
     (map (lambda (u) (outer p u alist k)) (get-union-values v))))
   ((constant-expression? p)
    ;; here I am: We will need to unroll this too.
    ;; needs work: To generate run-time equivalence check when the constant
    ;;             expression parameter and/or argument contain abstract
    ;;             booleans or abstract reals. When we do so, we need to call
    ;;             c:widen appropriately. These would correspond to the calls A
    ;;             to widen-abstract-value in abstract-destructure.
    (if (abstract-value-nondisjoint?
	 (concrete-value->abstract-value (constant-expression-value p)) v)
	(k alist)
	(new-panic-unit
	 (format #f "Argument is not an equivalent value for ~s"
		 (externalize-expression p)))))
   ((lambda-expression? p)
    (if (and (nonrecursive-closure? v)
	     (dereferenced-expression-eqv?
	      p (nonrecursive-closure-lambda-expression v)))
	(let inner ((xs1 (parameter-variables p))
		    (xs2 (nonrecursive-closure-variables v))
		    (vs (get-nonrecursive-closure-values v))
		    (alist alist)
		    (k k))
	 (if (null? xs1)
	     (k alist)
	     (outer (new-variable-access-expression (first xs1))
		    (first vs)
		    alist
		    (lambda (alist)
		     (inner (rest xs1) (rest xs2) (rest vs) alist k)))))
	(new-panic-unit
	 (format #f "Argument is not a matching nonrecursive closure for ~s"
		 (externalize-expression p)))))
   ((letrec-expression? p)
    (assert (and (variable-access-expression? (letrec-expression-body p))
		 (memp variable=?
		       (variable-access-expression-variable
			(letrec-expression-body p))
		       (letrec-expression-procedure-variables p))))
    (if (and (recursive-closure? v)
	     (= (recursive-closure-index v)
		(positionp variable=?
			   (variable-access-expression-variable
			    (letrec-expression-body p))
			   (letrec-expression-procedure-variables p)))
	     (= (vector-length
		 (recursive-closure-procedure-variables v))
		(length (letrec-expression-procedure-variables p)))
	     (= (vector-length
		 (recursive-closure-lambda-expressions v))
		(length (letrec-expression-lambda-expressions p)))
	     (every dereferenced-expression-eqv?
		    (vector->list (recursive-closure-lambda-expressions v))
		    (letrec-expression-lambda-expressions p)))
	(let inner ((xs1 (parameter-variables p))
		    (xs2 (recursive-closure-variables v))
		    (vs (get-recursive-closure-values v))
		    (alist alist)
		    (k k))
	 (if (null? xs1)
	     (k alist)
	     (outer (new-variable-access-expression (first xs1))
		    (first vs)
		    alist
		    (lambda (alist)
		     (inner (rest xs1) (rest xs2) (rest vs) alist k)))))
	(new-panic-unit
	 (format #f "Argument is not a matching recursive closure for ~s"
		 (externalize-expression p)))))
   ((cons-expression? p)
    (if (and (tagged-pair? v)
	     (equal-tags? (cons-expression-tags p) (tagged-pair-tags v)))
	(outer (cons-expression-car p)
	       (get-tagged-pair-car v)
	       alist
	       (lambda (alist)
		(outer (cons-expression-cdr p)
		       (get-tagged-pair-cdr v)
		       alist
		       k)))
	(new-panic-unit
	 (format #f "Argument is not a matching tagged pair with tags ~s"
		 (cons-expression-tags p)))))
   (else (internal-error)))))

(define (symbolic-apply v1 v2 top? function-instances)
 ;; needs work: We don't check the "Argument has wrong type for target"
 ;;             condition.
 (cond
  ((and (unit? v1)
	(or (union? (unit-abstract-value v1))
	    (primitive-procedure? (unit-abstract-value v1))
	    (closure? (unit-abstract-value v1))))
   (symbolic-apply (unroll v1) v2 top? function-instances))
  ((union? v1)
   (create-tagged-union
    (union-tag v1)
    (map (lambda (u1) (symbolic-apply u1 v2 #f function-instances))
	 (get-union-values v1))))
  ((primitive-procedure? v1)
   ((primitive-procedure-symbolic v1) v2 function-instances))
  ((closure? v1)
   (let ((v1-abstract (abstractify v1)) (v2-abstract (abstractify v2)))
    (cond
     ((and (not top?)
	   (not (function-instance-inline?
		 (findp function-instance=?
			(make-function-instance v1-abstract v2-abstract #t)
			function-instances))))
      (new-call-unit
       (with-abstract (lambda () (abstract-apply v1-abstract v2-abstract)))
       (c:function-name v1-abstract v2-abstract function-instances)
       v1
       v2))
     ((every-value-tags
       (lambda (tags2) (prefix-tags? (value-tags v1-abstract) tags2))
       v2-abstract)
      (symbolic-destructure
       (closure-parameter v1)
       v2
       (lambda (alist)
	(symbolic-eval (closure-body v1)
		       (construct-environment v1 alist)
		       function-instances))))
     ((some-value-tags
       (lambda (tags2) (prefix-tags? (value-tags v1-abstract) tags2))
       v2-abstract)
      (symbolic-destructure
       (closure-parameter v1)
       v2
       (lambda (alist)
	(symbolic-eval (closure-body v1)
		       (construct-environment v1 alist)
		       function-instances))))
     (else (new-panic-unit "Argument has wrong type for target")))))
  (else (new-panic-unit "Target is not a procedure"))))

(define (symbolic-eval e vs function-instances)
 (cond
  ((constant-expression? e) (constant-expression-value e))
  ((variable-access-expression? e) (first vs))
  ((lambda-expression? e) (new-nonrecursive-closure vs e))
  ((application? e)
   (if (lambda-expression? (application-callee e))
       ;; This handling of LET is an optimization. See the note in
       ;; concrete-eval.
       ;; needs work: We don't check the "Argument has wrong type for target"
       ;;             condition.
       (let* ((e1 (lambda-expression-body (application-callee e)))
	      (p (lambda-expression-parameter (application-callee e)))
	      (tags1 (lambda-expression-tags (application-callee e)))
	      (v (symbolic-eval
		  (application-argument e)
		  (restrict-environment vs (application-argument-indices e))
		  function-instances))
	      (v-abstract (abstractify v)))
	(cond
	 ((every-value-tags (lambda (tags) (prefix-tags? tags1 tags))
			    v-abstract)
	  (symbolic-destructure
	   p
	   v
	   (lambda (alist)
	    (symbolic-eval e1
			   (construct-environment-for-let e vs alist)
			   function-instances))))
	 ((some-value-tags (lambda (tags) (prefix-tags? tags1 tags))
			   v-abstract)
	  (symbolic-destructure
	   p
	   v
	   (lambda (alist)
	    (symbolic-eval e1
			   (construct-environment-for-let e vs alist)
			   function-instances))))
	 (else (new-panic-unit "Value has wrong type for let binder"))))
       (symbolic-apply
	(symbolic-eval
	 (application-callee e)
	 (restrict-environment vs (application-callee-indices e))
	 function-instances)
	(symbolic-eval
	 (application-argument e)
	 (restrict-environment vs (application-argument-indices e))
	 function-instances)
	#f
	function-instances)))
  ((letrec-expression? e)
   (symbolic-eval (letrec-expression-body e)
		  (letrec-nested-environment vs e)
		  function-instances))
  ((cons-expression? e)
   (let* ((v1 (symbolic-eval
	       (cons-expression-car e)
	       (restrict-environment vs (cons-expression-car-indices e))
	       function-instances))
	  (v2 (symbolic-eval
	       (cons-expression-cdr e)
	       (restrict-environment vs (cons-expression-cdr-indices e))
	       function-instances))
	  (v1-abstract (abstractify v1))
	  (v2-abstract (abstractify v2)))
    ;; needs work: We don't check the "Argument has wrong type for target"
    ;;             condition.
    (cond
     ((every-value-tags
       (lambda (tags1) (prefix-tags? (cons-expression-tags e) tags1))
       v1-abstract)
      (cond
       ((every-value-tags
	 (lambda (tags2) (prefix-tags? (cons-expression-tags e) tags2))
	 v2-abstract)
	(new-tagged-pair (cons-expression-tags e) v1 v2))
       ((some-value-tags
	 (lambda (tags2) (prefix-tags? (cons-expression-tags e) tags2))
	 v2-abstract)
	(new-tagged-pair (cons-expression-tags e) v1 v2))
       (else (new-panic-unit
	      (format #f "CDR argument has wrong type for target with tags ~s"
		      (cons-expression-tags e))))))
     ((some-value-tags
       (lambda (tags1) (prefix-tags? (cons-expression-tags e) tags1))
       v1-abstract)
      (cond
       ((every-value-tags
	 (lambda (tags2) (prefix-tags? (cons-expression-tags e) tags2))
	 v2-abstract)
	(new-tagged-pair (cons-expression-tags e) v1 v2))
       ((some-value-tags
	 (lambda (tags2) (prefix-tags? (cons-expression-tags e) tags2))
	 v2-abstract)
	(new-tagged-pair (cons-expression-tags e) v1 v2))
       (else (new-panic-unit
	      (format #f "CDR argument has wrong type for target with tags ~s"
		      (cons-expression-tags e))))))
     (else (new-panic-unit
	    (format #f "CAR argument has wrong type for target with tags ~s"
		    (cons-expression-tags e)))))))
  (else (internal-error))))

;;; Code Generator

;;; Identifiers
;;; x  argument for add#, minus#, times#, divide#, atantwo#, eq#, lt#
;;;    gt#, le#, ge#, iszero#, positive#, negative#, if_procedure#,
;;;    real#, write_real, write#, zero#, primal#, tangent#, and bundle#
;;; x  argument for unioner
;;; x  argument for widener
;;; x  result value in read_real
;;; x# variable name; # is variable index
;;; x# variable slot of closure struct; # is variable index
;;; x# letrec binding; # is variable index
;;; y  target temporary in function call dispatch
;;; u# union name; # is c:index
;;; s# struct name; # is c:index
;;;    union member name; # is c:index of member
;;; p  primal slot of bundle struct and slots of perturbation, sensitivity, and
;;;    reverse tagged value structs
;;; t  tangent slot of bundle struct
;;;    tag slot of a union
;;; a  car slot of pair struct
;;; d  cdr slot of pair struct
;;; w# widener name;  # is index of the widener-instance in widener-instances
;;; f# function name; # is index of the function-instance in function-instances
;;; m# constructor name; # is c:index of value being constructed
;;; m#_# unioner name; first # is c:index of value being
;;       constructed; second # is c:index of argument
;;; r  result value in constructor definition
;;; c  environment argument for f#
;;; The following are primitive names; # is c:index of argument
;;; add#
;;; minus#
;;; times#
;;; divide#
;;; atantwo#
;;; eq#
;;; lt#
;;; gt#
;;; le#
;;; ge#
;;; iszero#
;;; positive#
;;; negative#
;;; if_procedure#
;;; read_real
;;; real#
;;; write_real
;;; write#
;;; zero#
;;; perturb#
;;; unperturb#
;;; primal#
;;; tangent#
;;; bundle#
;;; sensitize#
;;; unsensitize#
;;; plus#
;;; starj#
;;; starj_inverse#
;;; main

;;; We box abstract values, not slots of aggregates, not arguments, not return
;;; values, not local variables, not type tags, and not unions.

;;; here I am: In all or almost all of the cases where we eliminate void
;;;            parameters or arguments or functions that return void results
;;;            we unsoundly removes code that might do I/O, signal an error, or
;;;            not terminate.

(define (void? v)
 (let ((p?
	(cond
	 ;; abstraction
	 ((eq? v 'int) #f)
	 ((union? v) (union-void? v))
	 ((abstract-real? v) #f)
	 ((scalar-value? v) #t)
	 ((nonrecursive-closure? v) (nonrecursive-closure-void? v))
	 ((recursive-closure? v) (recursive-closure-void? v))
	 ((perturbation-tagged-value? v) (perturbation-tagged-value-void? v))
	 ((bundle? v) (bundle-void? v))
	 ((sensitivity-tagged-value? v) (sensitivity-tagged-value-void? v))
	 ((reverse-tagged-value? v) (reverse-tagged-value-void? v))
	 ((tagged-pair? v) (tagged-pair-void? v))
	 (else (internal-error)))))
  (assert (boolean? p?))
  p?))

(define (deep-void? v)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  35
  (let loop ((v v) (cs '()) (k (lambda (r? cs) r?)))
   (cond ((memq v cs) (k #t cs))
	 ((union? v)
	  ;; The empty abstract value is not considered void. This is because
	  ;; void parameter/arguments are eliminated and we cannot do that for
	  ;; code that will issue an error. That is why the following is = and
	  ;; not <=.
	  (if (= (length (get-union-values v)) 1)
	      (every-cps loop (get-union-values v) (cons v cs) k)
	      (k #f cs)))
	 ((abstract-real? v) (k #f cs))
	 ((scalar-value? v) (k #t cs))
	 (else (every-cps loop (aggregate-value-values v) (cons v cs) k))))))

(define (determine-void?!)
 (for-each
  (lambda (e)
   (when (lambda-expression? e)
    (for-each (lambda (v) (set-nonrecursive-closure-void?! v (deep-void? v)))
	      (lambda-expression-nonrecursive-closures e))
    (for-each (lambda (v) (set-recursive-closure-void?! v (deep-void? v)))
	      (lambda-expression-recursive-closures e))))
  *expressions*)
 (for-each (lambda (v) (set-perturbation-tagged-value-void?! v (deep-void? v)))
	   *perturbation-tagged-values*)
 (for-each (lambda (v) (set-bundle-void?! v (deep-void? v))) *bundles*)
 (for-each (lambda (v) (set-sensitivity-tagged-value-void?! v (deep-void? v)))
	   *sensitivity-tagged-values*)
 (for-each (lambda (v) (set-reverse-tagged-value-void?! v (deep-void? v)))
	   *reverse-tagged-values*)
 (for-each (lambda (v) (set-tagged-pair-void?! v (deep-void? v)))
	   *tagged-pairs*)
 (for-each (lambda (v) (set-union-void?! v (deep-void? v))) *unions*))

(define (union-abstract-values vs1 vs2) (unionp abstract-value=? vs1 vs2))

(define (all-abstract-values)
 (map-reduce
  union-abstract-values
  '()
  (lambda (e)
   (map-reduce union-abstract-values
	       '()
	       (lambda (b)
		(remove-duplicatesp abstract-value=?
				    (cons (environment-binding-value b)
					  (environment-binding-values b))))
	       (expression-environment-bindings e)))
  *expressions*))

(define (all-unary-abstract-subvalues descend? v)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  36
  (let outer ((v v) (vs '()) (n '()) (k (lambda (n vs) n)))
   (cond
    ((memq v vs) (k n vs))
    ((union? v)
     (let inner ((us (get-union-values v))
		 (vs (cons v vs))
		 (n (adjoinp abstract-value=? v n)))
      (if (null? us)
	  (k n vs)
	  (outer (first us) vs n (lambda (n vs) (inner (rest us) vs n))))))
    ;; here I am: Need to return an empty abstract value for certain inputs to
    ;;            certain AD primitives.
    ((or (scalar-value? v) (not (descend? v)))
     (k (adjoinp abstract-value=? v n) vs))
    (else (let inner ((vs1 (aggregate-value-values v))
		      (vs (cons v vs))
		      (n (adjoinp abstract-value=? v n)))
	   (if (null? vs1)
	       (k n vs)
	       (outer (first vs1)
		      vs
		      n
		      (lambda (n vs) (inner (rest vs1) vs n))))))))))

(define (all-binary-abstract-subvalues
	 descend? f? f f-inverse aggregates-match? v)
 ;; This is written in CPS so as not to break structure sharing.
 ;; here I am: The results of f and f-inverse might violate the syntactic
 ;;            constraints.
 (define (outer1 v vs cs n k)
  (cond ((memq v vs) (k n vs cs))
	((union? v)
	 (let inner ((us (get-union-values v))
		     (vs (cons v vs))
		     (cs cs)
		     (n (adjoinp abstract-value=? v n)))
	  (if (null? us)
	      (k n vs cs)
	      (outer1 (first us)
		      vs
		      cs
		      n
		      (lambda (n vs cs) (inner (rest us) vs cs n))))))
	;; needs work: Do we need to update vs here?
	((vlad-pair? v) (outer2 (vlad-car v) (vlad-cdr v) vs cs n k))
	(else (k n vs cs))))
 (define (outer2 v1 v2 vs cs n k)
  (cond ((some (lambda (c) (and (eq? (car c) v1) (eq? (cdr c) v2))) cs)
	 (k n vs cs))
	((union? v1)
	 (let inner ((us (get-union-values v1))
		     (vs vs)
		     (cs (cons (cons v1 v2) cs))
		     (n (adjoinp abstract-value=? (vlad-cons v1 v2) n)))
	  (if (null? us)
	      (k n vs cs)
	      (outer2 (first us)
		      v2
		      vs
		      cs
		      n
		      (lambda (n vs cs)
		       ;; This is needed because of a bug in Scheme->C
		       (let ((result (inner (rest us) vs cs n))) result))))))
	((union? v2)
	 (let inner ((us (get-union-values v2))
		     (vs vs)
		     (cs (cons (cons v1 v2) cs))
		     (n (adjoinp abstract-value=? (vlad-cons v1 v2) n)))
	  (if (null? us)
	      (k n vs cs)
	      (outer2 v1
		      (first us)
		      vs
		      cs
		      n
		      (lambda (n vs cs) (inner (rest us) vs cs n))))))
	;; The calls to f and f-inverse should never return an empty abstract
	;; value. The call to f-inverse might issue "might" warnings.
	((and (f? v2) (union? (f-inverse v2)))
	 (let inner ((us (get-union-values (f-inverse v2)))
		     (vs vs)
		     (cs (cons (cons v1 v2) cs))
		     (n (adjoinp abstract-value=? (vlad-cons v1 v2) n)))
	  (if (null? us)
	      (k n vs cs)
	      (outer2 v1
		      (f (first us))
		      vs
		      cs
		      n
		      (lambda (n vs cs) (inner (rest us) vs cs n))))))
	;; here I am: Need to return an empty abstract value for nonconforming
	;;            inputs.
	((or (scalar-value? v1) (not (descend? v1)))
	 (k (adjoinp abstract-value=? (vlad-cons v1 v2) n) vs cs))
	((aggregates-match? v1 v2)
	 (let inner ((vs1 (aggregate-value-values v1))
		     (vs2 (aggregate-value-values v2))
		     (vs vs)
		     (cs (cons (cons v1 v2) cs))
		     (n (adjoinp abstract-value=? (vlad-cons v1 v2) n)))
	  (if (null? vs1)
	      (k n vs cs)
	      (outer2
	       (first vs1)
	       (first vs2)
	       vs
	       cs
	       n
	       (lambda (n vs cs) (inner (rest vs1) (rest vs2) vs cs n))))))
	;; here I am: Need to return an empty abstract value for nonconforming
	;;            inputs.
	(else (k (adjoinp abstract-value=? (vlad-cons v1 v2) n) vs cs))))
 (time-it-bucket
  37
  (outer1 v '() '() '() (lambda (n vs cs) n))))

(define (feedback-topological-sort vertex=? before choose vertices)
 ;; Minimal feedback set is the problem of computing the smallest set of edges
 ;; to remove from a digraph to make it acyclic. It is NP complete. This solves
 ;; a related problem of finding a minimal set of vertices to remove from a
 ;; digraph to make it acyclic. I don't know if this problem is NP hard. This
 ;; is a greedy heuristic for solving this problem. It partitions vertices into
 ;; two sets, vertices1 and vertices2, where vertices2 is the set of removed
 ;; vertices and vertices1 is topologically sorted. (before vertex) returns the
 ;; vertices that must come before vertex.
 (let loop ((vertices vertices)
	    (vertices1 '())
	    (vertices2 '())
	    ;; edges is a list of pairs (vertex1 vertex2) where vertex1 must
	    ;; come before vertex2.
	    (edges (map-reduce
		    append
		    '()
		    (lambda (vertex2)
		     (map (lambda (vertex1) (list vertex1 vertex2))
			  (remove-if-not
			   (lambda (vertex1) (memp vertex=? vertex1 vertices))
			   (before vertex2))))
		    vertices)))
  ;; Each time through the loop the graph only contains edges that both enter
  ;; and leave vertices in vertices.
  (if (null? vertices)
      (list (reverse vertices1) vertices2)
      ;; vertices-prime is the set of vertices in vertices with no in-edges.
      (let ((vertices-prime
	     (set-differencep vertex=? vertices (map second edges))))
       (if (null? vertices-prime)
	   ;; Each time through the loop the graph only contains edges that
	   ;; both enter and leave vertices in l.
	   (let ((vertex
		  (let loop ((vertices vertices) (edges edges))
		   ;; vertices-prime is the set of vertices in vertices with
		   ;; out-edges.
		   (let ((vertices-prime
			  (intersectionp
			   vertex=? vertices (map first edges))))
		    (if (= (length vertices) (length vertices-prime))
			(choose vertices)
			(loop vertices-prime
			      ;; Update the graph to contain only edges that
			      ;; leave vertices in vertices-prime which is
			      ;; the new vertices.
			      (remove-if-not
			       (lambda (edge)
				(memp vertex=? (second edge) vertices-prime))
			       edges)))))))
	    (loop (removep vertex=? vertex vertices)
		  vertices1
		  (cons vertex vertices2)
		  ;; We are removing vertex from vertices so remove all edges
		  ;; entering or leaving vertex.
		  (remove-if (lambda (edge)
			      (or (vertex=? (first edge) vertex)
				  (vertex=? (second edge) vertex)))
			     edges)))
	   (loop (set-differencep vertex=? vertices vertices-prime)
		 (append vertices-prime vertices1)
		 vertices2
		 ;; We are removing vertices-prime from vertices so remove all
		 ;; edges leaving vertices-prime. Since vertices-prime=
		 ;; (set-differenceq vertices (map second edges)) there
		 ;; can be no edges entering vertices-prime.
		 (remove-if
		  (lambda (edge) (memp vertex=? (first edge) vertices-prime))
		  edges)))))))

(define (all-unary-ad s descend?)
 ;; This is called redundantly thrice, once in all-unary-ad-widener-instances,
 ;; once to generate declarations, and once to generate definitions.
 (map-reduce
  union-abstract-values
  '()
  (lambda (v) (all-unary-abstract-subvalues descend? v))
  (map-reduce
   union-abstract-values
   '()
   (lambda (e)
    (if (application? e)
	(remove-duplicatesp
	 abstract-value=?
	 (removeq #f
		  (map (lambda (b)
			(let ((v1 (abstract-eval1
				   (application-callee e)
				   (restrict-environment
				    (environment-binding-values b)
				    (application-callee-indices e)))))
			 (if (and (primitive-procedure? v1)
				  (eq? (primitive-procedure-name v1) s))
			     (abstract-eval1
			      (application-argument e)
			      (restrict-environment
			       (environment-binding-values b)
			       (application-argument-indices e)))
			     #f)))
		       (expression-environment-bindings e))))
	'()))
   *expressions*)))

(define (abstract-values-before v)
 (cond ((union? v) (get-union-values v))
       ((scalar-value? v) '())
       (else (aggregate-value-values v))))

(define (all-sorted-unary-ad s descend?)
 ;; This is called redundantly twice, once to generate declarations and once to
 ;; generate definitions.
 ;; This topological sort is needed so that all INLINE definitions come before
 ;; their uses as required by gcc.
 (feedback-topological-sort
  ;; here I am: Need better choice function.
  abstract-value=? abstract-values-before first (all-unary-ad s descend?)))

(define (all-binary-ad s descend? f? f f-inverse aggregates-match?)
 ;; This is called redundantly thrice, once in all-binary-ad-widener-instances,
 ;; once to generate declarations, and once to generate definitions.
 (map-reduce
  union-abstract-values
  '()
  (lambda (v)
   (all-binary-abstract-subvalues descend? f? f f-inverse aggregates-match? v))
  (map-reduce
   union-abstract-values
   '()
   (lambda (e)
    (if (application? e)
	(remove-duplicatesp
	 abstract-value=?
	 (removeq #f
		  (map (lambda (b)
			(let ((v1 (abstract-eval1
				   (application-callee e)
				   (restrict-environment
				    (environment-binding-values b)
				    (application-callee-indices e)))))
			 (if (and (primitive-procedure? v1)
				  (eq? (primitive-procedure-name v1) s))
			     (abstract-eval1
			      (application-argument e)
			      (restrict-environment
			       (environment-binding-values b)
			       (application-argument-indices e)))
			     #f)))
		       (expression-environment-bindings e))))
	'()))
   *expressions*)))

(define (all-sorted-binary-ad
	 s descend? f? f f-inverse aggregates-match? before)
 ;; This is called redundantly twice, once to generate declarations and once to
 ;; generate definitions.
 ;; This topological sort is needed so that all INLINE definitions come before
 ;; their uses as required by gcc.
 (feedback-topological-sort
  abstract-value=?
  before
  ;; here I am: Need better choice function.
  first
  (all-binary-ad s descend? f? f f-inverse aggregates-match?)))

(define (binary-ad-argument-and-result-abstract-values g vs)
 ;; here I am: The result of g might violate syntactic constraints.
 (map-reduce union-abstract-values
	     '()
	     ;; The call to gmight issue "might" warnings and might return an
	     ;; empty abstract value.
	     (lambda (v) (union-abstract-values (list v) (list (g v))))
	     vs))

(define (bundle-aggregates-match? v1 v2)
 (and (or (and (nonrecursive-closure? v1)
	       (nonrecursive-closure? v2)
	       (perturbation-parameter? (nonrecursive-closure-parameter v2))
	       (dereferenced-nonrecursive-closure-match? v1 (unperturb v2)))
	  (and (recursive-closure? v1)
	       (recursive-closure? v2)
	       (perturbation-parameter? (recursive-closure-parameter v2))
	       (dereferenced-recursive-closure-match? v1 (unperturb v2)))
	  (and (tagged-pair? v1)
	       (tagged-pair? v2)
	       (tagged? 'perturbation (tagged-pair-tags v2))
	       (equal-tags? (tagged-pair-tags v1)
			    (remove-tag 'perturbation (tagged-pair-tags v2)))))
      (not (empty-abstract-value? (bundle (vlad-cons v1 v2))))))

(define (plus-aggregates-match? v1 v2)
 (and (or (and (nonrecursive-closure? v1)
	       (nonrecursive-closure? v2)
	       (dereferenced-nonrecursive-closure-match? v1 v2))
	  (and (recursive-closure? v1)
	       (recursive-closure? v2)
	       (dereferenced-recursive-closure-match? v1 v2))
	  (and (perturbation-tagged-value? v1) (perturbation-tagged-value? v2))
	  (and (bundle? v1) (bundle? v2))
	  (and (sensitivity-tagged-value? v1) (sensitivity-tagged-value? v2))
	  (and (reverse-tagged-value? v1) (reverse-tagged-value? v2))
	  (and (tagged-pair? v1)
	       (tagged-pair? v2)
	       (equal-tags? (tagged-pair-tags v1) (tagged-pair-tags v2))))
      (not (empty-abstract-value? (plus (vlad-cons v1 v2))))))

(define (all-real*real-abstract-subvalues v)
 (union-abstract-values
  (list v)
  (cond
   ((union? v)
    (map-reduce union-abstract-values
		'()
		all-real*real-abstract-subvalues
		(get-union-values v)))
   ((vlad-pair? v)
    (let ((v1 (vlad-car v)) (v2 (vlad-cdr v)))
     (cond
      ((union? v1)
       (map-reduce
	union-abstract-values
	'()
	(lambda (u1) (all-real*real-abstract-subvalues (vlad-cons u1 v2)))
	(get-union-values v1)))
      ((union? v2)
       (map-reduce
	union-abstract-values
	'()
	(lambda (u2) (all-real*real-abstract-subvalues (vlad-cons v1 u2)))
	(get-union-values v2)))
      ((and (vlad-real? v1) (vlad-real? v2)) '())
      (else '()))))
   (else '()))))

(define (all-real-abstract-subvalues v)
 (union-abstract-values
  (list v)
  (cond ((union? v)
	 (map-reduce union-abstract-values
		     '()
		     all-real-abstract-subvalues
		     (get-union-values v)))
	((vlad-real? v) '())
	(else '()))))

(define (all-nested-abstract-values widener-instances)
 (feedback-topological-sort
  abstract-value=?
  abstract-values-before
  (lambda (vs) (or (find-if backpropagator? vs) (first vs)))
  (adjoinp
   abstract-value=?
   ;; We are lazy and always generate this. This is needed to generate
   ;; real_real and write_real when the program otherwise has no abstract
   ;; reals.
   (abstract-real)
   (adjoinp
    abstract-value=?
    ;; We are lazy and always generate this. This is only needed if (a
    ;; potentially internal recursive call to) an AD primitive can give a
    ;; run-time error. All non-AD primitives that issue errors have C return
    ;; types corresponding to (abstract-real) or (abstract-boolean). And
    ;; currently we don't handle programs that contain expressions with empty
    ;; abstract values. Such can result from non-AD primitives that return
    ;; empty abstract values (even though the C code generated for those
    ;; primitives has non-empty return types). And from AD primitives that
    ;; return empty abstract values. And from destructuring errors. And from
    ;; invalid calls. And from calls that can never return (either because they
    ;; yield errors or involve infinite recursion). Flow analysis will yield an
    ;; empy abstract value in all of the above cases except for an internal
    ;; recursive call to an AD primitive. all-binary-abstract-subvalues doesn't
    ;; currently handle the cases requiring returning of empty abstract values.
    ;; binary-ad-argument-and-result-abstract-values does handle this case.
    ;; And currently, indicated by the comment below, unary AD primitives are
    ;; handled by a mechanism that is not aware of the particular error
    ;; characteristics of each primitive. And neither is
    ;; all-unary-abstract-subvalues. So for now we punt and always generate the
    ;; empty abstract value.
    (empty-abstract-value)
    (map-reduce
     union-abstract-values
     '()
     (lambda (v) (all-unary-abstract-subvalues (lambda (v) #t) v))
     ;; This assumes that the abstract values of the arguments of all internal
     ;; recursive calls to AD primitives are generated through
     ;; all-unary-abstract-subvalues.
     (union-abstract-values
      (union-abstract-values
       (union-abstract-values
	(union-abstract-values
	 (binary-ad-argument-and-result-abstract-values
	  bundle
	  (all-binary-ad 'bundle
			 (lambda (v)
			  (and (not (perturbation-tagged-value? v))
			       (not (bundle? v))
			       (not (sensitivity-tagged-value? v))
			       (not (reverse-tagged-value? v))))
			 perturbation-tagged-value? perturb unperturb
			 bundle-aggregates-match?))
	 (binary-ad-argument-and-result-abstract-values
	  plus
	  (all-binary-ad 'plus (lambda (v) #t) (lambda (v) #f) identity
			 identity plus-aggregates-match?)))
	(all-abstract-values))
       (union-abstract-values
	(map-reduce union-abstract-values
		    '()
		    (lambda (s)
		     (map-reduce union-abstract-values
				 '()
				 all-real*real-abstract-subvalues
				 (all-primitives s)))
		    '(+ - * / atan = < > <= >=))
	(map-reduce
	 union-abstract-values
	 '()
	 (lambda (s)
	  (map-reduce union-abstract-values
		      '()
		      all-real-abstract-subvalues
		      (all-primitives s)))
	 '(sqrt exp log sin cos zero? positive? negative? real write))))
      (union-abstract-values
       (map-reduce
	union-abstract-values
	'()
	(lambda (widener-instance)
	 (list (widener-instance-v1 widener-instance)))
	(append (first widener-instances) (second widener-instances)))
       (map-reduce
	union-abstract-values
	'()
	(lambda (widener-instance)
	 (list (widener-instance-v2 widener-instance)))
	(append (first widener-instances) (second widener-instances))))))))))

(define (function-instance=? function-instance1 function-instance2)
 (and (abstract-value=? (function-instance-v1 function-instance1)
			(function-instance-v1 function-instance2))
      (abstract-value=? (function-instance-v2 function-instance1)
			(function-instance-v2 function-instance2))))

(define (union-function-instances function-instances1 function-instances2)
 (unionp function-instance=? function-instances1 function-instances2))

(define (if-return-value v)
 (map-union
  (lambda (u)
   (if (vlad-pair? u)
       (let ((v1 (vlad-car u)))
	(map-union (lambda (u23)
		    (if (vlad-pair? u23)
			(let ((v2 (vlad-car u23)) (v3 (vlad-cdr u23)))
			 (cond ((and (some vlad-false? (union-members v1))
				     (some (lambda (u1) (not (vlad-false? u1)))
					   (union-members v1)))
				;; here I am: The result might violate the
				;;            syntactic constraints.
				(abstract-value-union
				 (abstract-apply v2 (vlad-empty-list))
				 (abstract-apply v3 (vlad-empty-list))))
			       ((some vlad-false? (union-members v1))
				(abstract-apply v3 (vlad-empty-list)))
			       ((some (lambda (u1) (not (vlad-false? u1)))
				      (union-members v1))
				(abstract-apply v2 (vlad-empty-list)))
			       (else (internal-error))))
			(empty-abstract-value)))
		   (vlad-cdr u)))
       (empty-abstract-value)))
  v))

(define (if-function-instances v)
 (if (void? (if-return-value v))
     ;; needs work: I'm not sure whether this should be moved into the
     ;;             individual make-function-instance calls.
     '()
     (map-reduce
      union-function-instances
      '()
      (lambda (u)
       (if (vlad-pair? u)
	   (let ((v1 (vlad-car u)))
	    (map-reduce
	     union-function-instances
	     '()
	     (lambda (u23)
	      (if (vlad-pair? u23)
		  (let ((v2 (vlad-car u23)) (v3 (vlad-cdr u23)))
		   (cond
		    ((and (some vlad-false? (union-members v1))
			  (some (lambda (u1) (not (vlad-false? u1)))
				(union-members v1)))
		     (union-function-instances
		      (map-reduce
		       union-function-instances
		       '()
		       (lambda (u2)
			(if (closure? u2)
			    (list
			     (make-function-instance u2 (vlad-empty-list) #t))
			    '()))
		       (union-members v2))
		      (map-reduce
		       union-function-instances
		       '()
		       (lambda (u3)
			(if (closure? u3)
			    (list
			     (make-function-instance u3 (vlad-empty-list) #t))
			    '()))
		       (union-members v3))))
		    ((some vlad-false? (union-members v1))
		     (map-reduce
		      union-function-instances
		      '()
		      (lambda (u3)
		       (if (closure? u3)
			   (list
			    (make-function-instance u3 (vlad-empty-list) #t))
			   '()))
		      (union-members v3)))
		    ((some (lambda (u1) (not (vlad-false? u1)))
			   (union-members v1))
		     (map-reduce
		      union-function-instances
		      '()
		      (lambda (u2)
		       (if (closure? u2)
			   (list
			    (make-function-instance u2 (vlad-empty-list) #t))
			   '()))
		      (union-members v2)))
		    (else (internal-error))))
		  '()))
	     (union-members (vlad-cdr u))))
	   '()))
      (union-members v))))

(define (all-function-instances)
 (map-reduce
  union-function-instances
  '()
  (lambda (e)
   (if (and (application? e)
	    ;; This handling of LET is an optimization.
	    (not (lambda-expression? (application-callee e))))
       (map-reduce
	union-function-instances
	'()
	(lambda (b)
	 (let ((v1 (abstract-eval1
		    (application-callee e)
		    (restrict-environment (environment-binding-values b)
					  (application-callee-indices e))))
	       (v2 (abstract-eval1
		    (application-argument e)
		    (restrict-environment (environment-binding-values b)
					  (application-argument-indices e)))))
	  (cond
	   ((union? v1)
	    (map-reduce
	     union-function-instances
	     '()
	     (lambda (u1)
	      (cond ((closure? u1) (list (make-function-instance u1 v2 #t)))
		    ((and (primitive-procedure? u1)
			  (eq? (primitive-procedure-name u1) 'if-procedure))
		     (if-function-instances v2))
		    (else '())))
	     (get-union-values v1)))
	   ((closure? v1) (list (make-function-instance v1 v2 #t)))
	   ((and (primitive-procedure? v1)
		 (eq? (primitive-procedure-name v1) 'if-procedure))
	    (if-function-instances v2))
	   (else '()))))
	(expression-environment-bindings e))
       '()))
  *expressions*))

(define (widener-instance=? widener-instance1 widener-instance2)
 (and (abstract-value=? (widener-instance-v1 widener-instance1)
			(widener-instance-v1 widener-instance2))
      (abstract-value=? (widener-instance-v2 widener-instance1)
			(widener-instance-v2 widener-instance2))))

(define (union-widener-instances widener-instances1 widener-instances2)
 (unionp widener-instance=? widener-instances1 widener-instances2))

(define (all-subwidener-instances v1 v2)
 ;; This is written in CPS so as not to break structure sharing.
 (time-it-bucket
  38
  (let outer ((v1 v1) (v2 v2) (cs '()) (n '()) (k (lambda (n cs) n)))
   (cond
    ((some (lambda (c) (and (eq? (car c) v1) (eq? (cdr c) v2))) cs) (k n cs))
    ((abstract-value=? v1 v2) (k n cs))
    ;; Note that we have syntactic constraints that widen (union r1 r2) to R
    ;; but not things like (union (perturbation r1) (perturbation r2)) to
    ;; (perturbation R). Because of this, v1 might be a union even though v2
    ;; might not be.
    ((union? v1)
     (let inner ((us (get-union-values v1))
		 (cs (cons (cons v1 v2) cs))
		 (n (adjoinp
		     widener-instance=? (make-widener-instance v1 v2) n)))
      (if (null? us)
	  (k n cs)
	  (outer (first us) v2 cs n (lambda (n cs) (inner (rest us) cs n))))))
    ;; If v2 is an empty abstract value then v1 will be and the first case
    ;; where (abstract-value=? v1 v2) will be taken and this case will never
    ;; be.
    ((union? v2)
     (outer v1
	    ;; The fact that such a u2 exists that is a member of v2 relies on
	    ;; our imprecise notion of abstract-value subset. There may be more
	    ;; than one. Any will do, it is only a matter of efficiency to
	    ;; choose between the alternatives. I don't even know how to
	    ;; define/determine which alternative would be most efficient.
	    (find-if (lambda (u2) (abstract-value-subset? v1 u2))
		     (get-union-values v2))
	    ;; needs work: Do we need to update cs here?
	    cs
	    (adjoinp widener-instance=? (make-widener-instance v1 v2) n)
	    k))
    ((scalar-value? v2)
     (k (adjoinp widener-instance=? (make-widener-instance v1 v2) n) cs))
    ;; This will only be done on conforming structures since the analysis is
    ;; almost union free.
    (else
     (let inner ((vs1 (aggregate-value-values v1))
		 (vs2 (aggregate-value-values v2))
		 (cs (cons (cons v1 v2) cs))
		 (n (adjoinp
		     widener-instance=? (make-widener-instance v1 v2) n)))
      (if (null? vs1)
	  (k n cs)
	  (outer (first vs1)
		 (first vs2)
		 cs
		 n
		 (lambda (n cs) (inner (rest vs1) (rest vs2) cs n))))))))))

(define (all-unary-ad-widener-instances s descend? f)
 ;; here I am: The result of f might violate the syntactic constraints.
 (map-reduce
  union-widener-instances
  '()
  (lambda (v)
   (if (union? v)
       (map-reduce union-widener-instances
		   '()
		   ;; The call to f might issue "might" warnings and might
		   ;; return an empty abstract value.
		   (lambda (u) (all-subwidener-instances (f u) (f v)))
		   (get-union-values v))
       '()))
  (all-unary-ad s descend?)))

(define (all-binary-ad-widener-instances
	 s descend? f? f f-inverse g aggregates-match?)
 ;; here I am: The results of f, f-inverse, and g might violate the
 ;;            syntactic constraints.
 (map-reduce
  union-widener-instances
  '()
  (lambda (v)
   (cond
    ((union? v)
     (map-reduce union-widener-instances
		 '()
		 ;; The calls to g might issue "might" warnings and might
		 ;; return empty abstract values.
		 (lambda (u) (all-subwidener-instances (g u) (g v)))
		 (get-union-values v)))
    ((vlad-pair? v)
     (let ((v1 (vlad-car v)) (v2 (vlad-cdr v)))
      (cond ((union? v1)
	     (map-reduce union-widener-instances
			 '()
			 ;; The calls to g might issue "might" warnings and
			 ;; might return empty abstract values.
			 (lambda (u1)
			  (all-subwidener-instances
			   (g (vlad-cons u1 v2)) (g (vlad-cons v1 v2))))
			 (get-union-values v1)))
	    ((union? v2)
	     (map-reduce union-widener-instances
			 '()
			 ;; The calls to g might issue "might" warnings and
			 ;; might return empty abstract values.
			 (lambda (u2)
			  (all-subwidener-instances
			   (g (vlad-cons v1 u2)) (g (vlad-cons v1 v2))))
			 (get-union-values v2)))
	    ;; The calls to f and f-inverse should never return an empty
	    ;; abstract value. The call to f-inverse might issue "might"
	    ;; warnings.
	    ((and (f? v2) (union? (f-inverse v2)))
	     (map-reduce union-widener-instances
			 '()
			 ;; The calls to g might issue "might" warnings and
			 ;; might return empty abstract values.
			 (lambda (u2)
			  (all-subwidener-instances
			   (g (vlad-cons v1 (f u2))) (g (vlad-cons v1 v2))))
			 (get-union-values (f-inverse v2))))
	    (else '()))))
    (else '())))
  (all-binary-ad s descend? f? f f-inverse aggregates-match?)))

(define (if-widener-instances v)
 (let ((v0 (if-return-value v)))
  (map-reduce
   union-widener-instances
   '()
   (lambda (u)
    (if (vlad-pair? u)
	(let ((v1 (vlad-car u)))
	 (map-reduce
	  union-widener-instances
	  '()
	  (lambda (u23)
	   (if (vlad-pair? u23)
	       (let ((v2 (vlad-car u23)) (v3 (vlad-cdr u23)))
		(cond
		 ((and (some vlad-false? (union-members v1))
		       (some (lambda (u1) (not (vlad-false? u1)))
			     (union-members v1)))
		  (union-widener-instances
		   (union-widener-instances
		    (map-reduce union-widener-instances
				'()
				(lambda (u2)
				 (all-subwidener-instances
				  (abstract-apply u2 (vlad-empty-list))
				  (abstract-apply v2 (vlad-empty-list))))
				(union-members v2))
		    (all-subwidener-instances
		     (abstract-apply v2 (vlad-empty-list)) v0))
		   (union-widener-instances
		    (map-reduce union-widener-instances
				'()
				(lambda (u3)
				 (all-subwidener-instances
				  (abstract-apply u3 (vlad-empty-list))
				  (abstract-apply v3 (vlad-empty-list))))
				(union-members v3))
		    (all-subwidener-instances
		     (abstract-apply v3 (vlad-empty-list)) v0))))
		 ((some vlad-false? (union-members v1))
		  (union-widener-instances
		   (map-reduce union-widener-instances
			       '()
			       (lambda (u3)
				(all-subwidener-instances
				 (abstract-apply u3 (vlad-empty-list))
				 (abstract-apply v3 (vlad-empty-list))))
			       (union-members v3))
		   (all-subwidener-instances
		    (abstract-apply v3 (vlad-empty-list)) v0)))
		 ((some (lambda (u1) (not (vlad-false? u1)))
			(union-members v1))
		  (union-widener-instances
		   (map-reduce union-widener-instances
			       '()
			       (lambda (u2)
				(all-subwidener-instances
				 (abstract-apply u2 (vlad-empty-list))
				 (abstract-apply v2 (vlad-empty-list))))
			       (union-members v2))
		   (all-subwidener-instances
		    (abstract-apply v2 (vlad-empty-list)) v0)))
		 (else (internal-error))))
	       '()))
	  (union-members (vlad-cdr u))))
	'()))
   (union-members v))))

(define (all-widener-instances function-instances)
 ;; This topological sort is needed so that all INLINE definitions come before
 ;; their uses as required by gcc.
 (feedback-topological-sort
  widener-instance=?
  (lambda (widener-instance)
   (cond
    ;; See the note for this case in all-subwidener-instances.
    ((union? (widener-instance-v1 widener-instance))
     (map (lambda (u1)
	   (make-widener-instance u1 (widener-instance-v2 widener-instance)))
	  (get-union-values (widener-instance-v1 widener-instance))))
    ;; See the notes for this case in all-subwidener-instances.
    ((union? (widener-instance-v2 widener-instance))
     (map (lambda (u2)
	   (make-widener-instance (widener-instance-v1 widener-instance) u2))
	  (get-union-values (widener-instance-v2 widener-instance))))
    ((scalar-value? (widener-instance-v2 widener-instance)) '())
    (else
     ;; This will only be done on conforming structures since the analysis is
     ;; almost union free.
     (map make-widener-instance
	  (aggregate-value-values (widener-instance-v1 widener-instance))
	  (aggregate-value-values (widener-instance-v2 widener-instance))))))
  ;; here I am: Need better choice function.
  first
  (union-widener-instances
   (map-reduce
    union-widener-instances
    '()
    (lambda (function-instance)
     (map-reduce
      union-widener-instances
      '()
      (lambda (alist)
       (all-subwidener-instances
	(abstract-eval1
	 (closure-body (function-instance-v1 function-instance))
	 (construct-environment
	  (function-instance-v1 function-instance) alist))
	(abstract-apply (function-instance-v1 function-instance)
			(function-instance-v2 function-instance))))
      (abstract-destructure
       (closure-parameter (function-instance-v1 function-instance))
       (function-instance-v2 function-instance))))
    function-instances)
   (union-widener-instances
    (map-reduce
     union-widener-instances
     '()
     (lambda (e)
      (map-reduce
       union-widener-instances
       '()
       (lambda (b)
	(let ((vs (environment-binding-values b)))
	 (cond
	  ((constant-expression? e)
	   (all-subwidener-instances
	    (constant-expression-value e) (environment-binding-value b)))
	  ((variable-access-expression? e) '())
	  ((lambda-expression? e)
	   (all-subwidener-instances
	    (new-nonrecursive-closure vs e) (environment-binding-value b)))
	  ((application? e)
	   (if (lambda-expression? (application-callee e))
	       ;; This handling of LET is an optimization.
	       (let ((e1 (lambda-expression-body (application-callee e)))
		     (p (lambda-expression-parameter (application-callee e)))
		     (tags1 (lambda-expression-tags (application-callee e)))
		     (v (abstract-eval1
			 (application-argument e)
			 (restrict-environment
			  vs (application-argument-indices e)))))
		(cond
		 ((every-value-tags
		   (lambda (tags) (prefix-tags? tags1 tags)) v)
		  (map-reduce
		   union-widener-instances
		   '()
		   (lambda (alist)
		    (all-subwidener-instances
		     (abstract-eval1
		      e1 (construct-environment-for-let e vs alist))
		     (environment-binding-value b)))
		   (abstract-destructure p v)))
		 ((some-value-tags
		   (lambda (tags) (prefix-tags? tags1 tags)) v)
		  (compile-time-warning
		   "Value might have wrong type for let binder" v)
		  (map-reduce
		   union-widener-instances
		   '()
		   (lambda (alist)
		    (all-subwidener-instances
		     (abstract-eval1
		      e1 (construct-environment-for-let e vs alist))
		     (environment-binding-value b)))
		   (abstract-destructure p v)))
		 (else (compile-time-warning
			"Value might have wrong type for let binder" v)
		       '())))
	       (let ((v1 (abstract-eval1 (application-callee e)
					 (restrict-environment
					  vs (application-callee-indices e))))
		     (v2 (abstract-eval1 (application-argument e)
					 (restrict-environment
					  vs
					  (application-argument-indices e)))))
		(union-widener-instances
		 (all-subwidener-instances
		  (abstract-apply v1 v2) (environment-binding-value b))
		 (cond ((union? v1)
			(map-reduce
			 union-widener-instances
			 '()
			 (lambda (u1)
			  (all-subwidener-instances
			   (abstract-apply u1 v2) (abstract-apply v1 v2)))
			 (get-union-values v1)))
		       ((closure? v1) '())
		       ((and (primitive-procedure? v1)
			     (eq? (primitive-procedure-name v1) 'if-procedure))
			(if-widener-instances v2))
		       (else '()))))))
	  ((letrec-expression? e)
	   (union-widener-instances
	    (all-subwidener-instances
	     (abstract-eval1
	      (letrec-expression-body e) (letrec-nested-environment vs e))
	     (environment-binding-value b))
	    (map-reduce
	     union-widener-instances
	     '()
	     (lambda (x)
	      (let ((v (new-recursive-closure
			(restrict-environment vs (letrec-expression-indices e))
			(list->vector
			 (letrec-expression-procedure-variables e))
			(list->vector (letrec-expression-lambda-expressions e))
			(positionp
			 variable=?
			 x
			 (letrec-expression-procedure-variables e)))))
	       (all-subwidener-instances v (widen-abstract-value v))))
	     (letrec-expression-procedure-variables e))))
	  ((cons-expression? e)
	   (all-subwidener-instances
	    (new-tagged-pair
	     (cons-expression-tags e)
	     (abstract-eval1
	      (cons-expression-car e)
	      (restrict-environment vs (cons-expression-car-indices e)))
	     (abstract-eval1
	      (cons-expression-cdr e)
	      (restrict-environment vs (cons-expression-cdr-indices e))))
	    (environment-binding-value b)))
	  (else (internal-error)))))
       (expression-environment-bindings e)))
     *expressions*)
    (reduce
     union-widener-instances
     (list
      (all-unary-ad-widener-instances 'zero (lambda (v) #t) zero)
      (all-unary-ad-widener-instances
       'perturb
       (lambda (v)
	(and (not (perturbation-tagged-value? v))
	     (not (bundle? v))
	     (not (sensitivity-tagged-value? v))
	     (not (reverse-tagged-value? v))))
       perturb)
      (all-unary-ad-widener-instances
       'unperturb
       (lambda (v)
	(and (perturbation-value? v) (not (perturbation-tagged-value? v))))
       unperturb)
      (all-unary-ad-widener-instances
       'primal (lambda (v) (and (forward-value? v) (not (bundle? v)))) primal)
      (all-unary-ad-widener-instances
       'tangent
       (lambda (v) (and (forward-value? v) (not (bundle? v))))
       tangent)
      (all-binary-ad-widener-instances
       'bundle
       (lambda (v)
	(and (not (perturbation-tagged-value? v))
	     (not (bundle? v))
	     (not (sensitivity-tagged-value? v))
	     (not (reverse-tagged-value? v))))
       perturbation-tagged-value? perturb unperturb bundle
       bundle-aggregates-match?)
      (all-unary-ad-widener-instances
       'sensitize
       (lambda (v)
	(and (not (perturbation-tagged-value? v))
	     (not (bundle? v))
	     (not (sensitivity-tagged-value? v))
	     (not (reverse-tagged-value? v))))
       sensitize)
      (all-unary-ad-widener-instances
       'unsensitize
       (lambda (v)
	(and (sensitivity-value? v) (not (sensitivity-tagged-value? v))))
       unsensitize)
      (all-binary-ad-widener-instances
       'plus (lambda (v) #t) (lambda (v) #f) identity identity plus
       plus-aggregates-match?)
      (all-unary-ad-widener-instances
       '*j
       (lambda (v)
	(and (not (perturbation-tagged-value? v))
	     (not (bundle? v))
	     (not (sensitivity-tagged-value? v))
	     (not (reverse-tagged-value? v))))
       *j)
      (all-unary-ad-widener-instances
       '*j-inverse
       (lambda (v) (and (reverse-value? v) (not (reverse-tagged-value? v))))
       *j-inverse))
     '())))))

(define (all-primitives s)
 (map-reduce
  union-abstract-values
  '()
  (lambda (e)
   (if (application? e)
       (remove-duplicatesp
	abstract-value=?
	(removeq
	 #f
	 (map (lambda (b)
	       (let ((v1 (abstract-eval1 (application-callee e)
					 (restrict-environment
					  (environment-binding-values b)
					  (application-callee-indices e))))
		     (v2 (abstract-eval1 (application-argument e)
					 (restrict-environment
					  (environment-binding-values b)
					  (application-argument-indices e)))))
		(if (and (not (union? v1))
			 (primitive-procedure? v1)
			 (eq? (primitive-procedure-name v1) s)
			 (not (void? (abstract-apply v1 v2))))
		    v2
		    #f)))
	      (expression-environment-bindings e))))
       '()))
  *expressions*))

(define (if-instance=? if-instance1 if-instance2)
 (abstract-value=? (if-instance-v if-instance1) (if-instance-v if-instance2)))

(define (if-and-function-instance=? instance1 instance2)
 (or (and (if-instance? instance1)
	  (if-instance? instance2)
	  (if-instance=? instance1 instance2))
     (and (function-instance? instance1)
	  (function-instance? instance2)
	  (function-instance=? instance1 instance2))))

(define (union-if-and-function-instances instances1 instances2)
 (unionp if-and-function-instance=? instances1 instances2))

(define (all-instances1-instances2 function-instances)
 ;; This topological sort is needed so that all INLINE definitions come before
 ;; their uses as required by gcc.
 (feedback-topological-sort
  if-and-function-instance=?
  (lambda (instance)
   (cond
    ((if-instance? instance) (if-function-instances (if-instance-v instance)))
    ((function-instance? instance)
     (map-reduce
      union-if-and-function-instances
      '()
      (lambda (alist)
       (let loop ((e (closure-body (function-instance-v1 instance)))
		  (vs (map widen-abstract-value
			   (construct-environment
			    (function-instance-v1 instance) alist))))
	(assert (= (length vs) (length (free-variables e))))
	(cond
	 ((application? e)
	  (if (lambda-expression? (application-callee e))
	      ;; This handling of LET is an optimization.
	      (let ((e1 (lambda-expression-body (application-callee e)))
		    (p (lambda-expression-parameter (application-callee e)))
		    (tags1 (lambda-expression-tags (application-callee e)))
		    (v (abstract-eval1
			(application-argument e)
			(restrict-environment
			 vs (application-argument-indices e)))))
	       (union-if-and-function-instances
		(cond
		 ((every-value-tags
		   (lambda (tags) (prefix-tags? tags1 tags)) v)
		  (map-reduce
		   union-if-and-function-instances
		   '()
		   (lambda (alist)
		    (loop e1 (construct-environment-for-let e vs alist)))
		   (abstract-destructure p v)))
		 ((some-value-tags (lambda (tags) (prefix-tags? tags1 tags)) v)
		  (compile-time-warning
		   "Value might have wrong type for let binder" v)
		  (map-reduce
		   union-if-and-function-instances
		   '()
		   (lambda (alist)
		    (loop e1 (construct-environment-for-let e vs alist)))
		   (abstract-destructure p v)))
		 (else (compile-time-warning
			"Value might have wrong type for let binder" v)
		       '()))
		(loop
		 (application-argument e)
		 (restrict-environment vs (application-argument-indices e)))))
	      (let ((v1 (abstract-eval1
			 (application-callee e)
			 (restrict-environment
			  vs (application-callee-indices e))))
		    (v2 (abstract-eval1
			 (application-argument e)
			 (restrict-environment
			  vs (application-argument-indices e)))))
	       (union-if-and-function-instances
		(map-reduce
		 union-if-and-function-instances
		 '()
		 (lambda (u1)
		  (cond
		   ((and (primitive-procedure? u1)
			 (eq? (primitive-procedure-name u1) 'if-procedure))
		    (list (make-if-instance v2)))
		   ((closure? u1) (list (make-function-instance u1 v2 #t)))
		   (else '())))
		 (union-members v1))
		(union-if-and-function-instances
		 (loop
		  (application-callee e)
		  (restrict-environment vs (application-callee-indices e)))
		 (loop (application-argument e)
		       (restrict-environment
			vs (application-argument-indices e))))))))
	 ((letrec-expression? e)
	  (loop (letrec-expression-body e)
		(map widen-abstract-value (letrec-nested-environment vs e))))
	 ((cons-expression? e)
	  (union-if-and-function-instances
	   (loop (cons-expression-car e)
		 (restrict-environment vs (cons-expression-car-indices e)))
	   (loop (cons-expression-cdr e)
		 (restrict-environment vs (cons-expression-cdr-indices e)))))
	 (else '()))))
      (abstract-destructure
       (closure-parameter (function-instance-v1 instance))
       (function-instance-v2 instance))))
    (else (internal-error))))
  (lambda (instances)
   (let ((instance
	  (or (find-if
	       (lambda (instance)
		(and (function-instance? instance)
		     (recursive-closure? (function-instance-v1 instance))))
	       instances)
	      (find-if
	       (lambda (instance)
		(and (function-instance? instance)
		     (backpropagator? (function-instance-v1 instance))))
	       instances)
	      (find-if function-instance? instances))))
    (assert instance)
    instance))
  (append (map make-if-instance (all-primitives 'if-procedure))
	  function-instances)))

(define (boxed? v)
 (cond ((nonrecursive-closure? v) (nonrecursive-closure-boxed? v))
       ((recursive-closure? v) (recursive-closure-boxed? v))
       ((perturbation-tagged-value? v) (perturbation-tagged-value-boxed? v))
       ((bundle? v) (bundle-boxed? v))
       ((sensitivity-tagged-value? v) (sensitivity-tagged-value-boxed? v))
       ((reverse-tagged-value? v) (reverse-tagged-value-boxed? v))
       ((tagged-pair? v) (tagged-pair-boxed? v))
       ((union? v) (union-boxed? v))
       (else #f)))

(define (c:index v)
 (cond ((nonrecursive-closure? v) (nonrecursive-closure-c:index v))
       ((recursive-closure? v) (recursive-closure-c:index v))
       ((perturbation-tagged-value? v) (perturbation-tagged-value-c:index v))
       ((bundle? v) (bundle-c:index v))
       ((sensitivity-tagged-value? v) (sensitivity-tagged-value-c:index v))
       ((reverse-tagged-value? v) (reverse-tagged-value-c:index v))
       ((tagged-pair? v) (tagged-pair-c:index v))
       ((union? v) (union-c:index v))
       (else (cdr (assp abstract-value=? v *c:indices*)))))

;;; Primitive C syntax generators

(define (c:parenthesize code) (list "(" code ")"))

(define (c:sizeof code) (list "sizeof" (c:parenthesize code)))

(define (c:pointer-cast code1 code2)
 (list (c:parenthesize (list code1 " " "*")) code2))

(define (c:binary code1 code2 code3) (list code1 code2 code3))

(define (c:conditional code1 code2 code3) (list code1 "?" code2 ":" code3))

(define (c:assignment code1 code2) (list code1 "=" code2 ";"))

(define (c:return code) (list "return" " " code ";"))

(define (c:variable-name x) (list "x" (variable-index x)))

(define (c:specifier v)
 ;; abstraction
 (cond ((eq? v 'int) "int")
       (else (assert (not (void? v)))
	     (if (and (not (union? v)) (abstract-real? v))
		 "double"
		 (list "struct" " " (list "s" (c:index v)))))))

(define (c:pointer-declarator code) (list "*" code))

(define (generate-slot-names v)
 ;; generate -~-> c:
 (cond
  ((union? v) (map (lambda (v) (list "s" (c:index v))) (get-union-values v)))
  ((nonrecursive-closure? v)
   (map c:variable-name (nonrecursive-closure-variables v)))
  ((recursive-closure? v)
   (map c:variable-name (recursive-closure-variables v)))
  ((perturbation-tagged-value? v) '("p"))
  ((bundle? v) '("p" "v"))
  ((sensitivity-tagged-value? v) '("p"))
  ((reverse-tagged-value? v) '("p"))
  ((tagged-pair? v) '("a" "d"))
  (else (internal-error))))

(define (c:parameter code1 code2) (list code1 " " code2))

(define (c:specifier-parameter v code)
 (if (void? v)
     '()
     (c:parameter (c:specifier v)
		  (if (boxed? v) (c:pointer-declarator code) code))))

(define (c:declaration code1 code2) (list code1 " " code2 ";"))

(define (c:specifier-declaration v code)
 (if (void? v)
     '()
     (c:declaration (c:specifier v)
		    (if (boxed? v) (c:pointer-declarator code) code))))

(define (c:init-declaration code1 code2 code3)
 (list code1 " " code2 "=" code3 ";"))

(define (c:specifier-init-declaration v code1 code2)
 (if (void? v)
     '()
     (c:init-declaration (c:specifier v)
			 (if (boxed? v) (c:pointer-declarator code1) code1)
			 code2)))

(define (c:statement-expression . codes)
 (c:parenthesize (list "{" codes ";" "}")))

(define (c:let v code1 code2 code3)
 (c:statement-expression (c:specifier-init-declaration v code1 code2) code3))

(define (generate-struct-and-union-declarations vs1-vs2)
 ;; generate -~-> c:
 ;; abstraction
 (list
  ;; This generates forward declarations for the struct and union tags.
  ;; abstraction
  (map (lambda (v)
	(cond ((void? v) '())
	      ((union? v)
	       ;; abstraction
	       (list
		(if (every void? (get-union-values v))
		    '()
		    ;; abstraction
		    (list *union*
			  " "
			  ;; abstraction
			  (list "u" (c:index v))
			  ";"
			  #\newline))
		;; abstraction
		(list (c:specifier v) ";" #\newline)))
	      ((abstract-real? v) '())
	      (else
	       ;; abstraction
	       (list
		;; abstraction
		(list (c:specifier v) ";" #\newline)))))
       (second vs1-vs2))
  ;; abstraction
  (map (lambda (v)
	(cond ((void? v) '())
	      ((union? v)
	       ;; By fortuitous confluence, this will eliminate the union
	       ;; declaration for the empty abstract value and generate a
	       ;; struct declaration with just a type tag (which will never be
	       ;; used).
	       ;; abstraction
	       (list
		(if (every void? (get-union-values v))
		    '()
		    ;; abstraction
		    (list *union*
			  " "
			  ;; abstraction
			  (list "u" (c:index v))
			  "{"
			  (map c:specifier-declaration
			       (get-union-values v)
			       (generate-slot-names v))
			  "}"
			  ";"
			  #\newline))
		;; abstraction
		(list (c:specifier v)
		      "{"
		      ;; The type tag is always unboxed.
		      (c:declaration "int" "v")
		      (if (every void? (get-union-values v))
			  '()
			  ;; The union is always unboxed.
			  (c:declaration
			   ;; abstraction
			   (list *union*
				 " "
				 ;; abstraction
				 (list "u" (c:index v)))
			   "u"))
		      "}"
		      ";"
		      #\newline)))
	      ((abstract-real? v) '())
	      (else
	       ;; abstraction
	       (list
		;; abstraction
		(list (c:specifier v)
		      "{"
		      (map c:specifier-declaration
			   (aggregate-value-values v)
			   (generate-slot-names v))
		      "}"
		      ";"
		      #\newline)))))
       (append (first vs1-vs2) (second vs1-vs2)))))

(define (c:builtin-name code v) (list code (c:index v)))

(define (c:constructor-name v) (list "m" (c:index v)))

(define (c:unioner-name u v) (list "m" (c:index v) "_" (c:index u)))

(define (c:function-name v1 v2 function-instances)
 (assert (memp function-instance=?
	       (make-function-instance v1 v2 #t)
	       function-instances))
 (list "f" (positionp function-instance=?
		      (make-function-instance v1 v2 #t)
		      function-instances)))

(define (c:widener-name v1 v2 widener-instances)
 (assert (memp widener-instance=?
	       (make-widener-instance v1 v2)
	       (append (first widener-instances) (second widener-instances))))
 (list
  "w"
  (positionp widener-instance=?
	     (make-widener-instance v1 v2)
	     (append (first widener-instances) (second widener-instances)))))

(define (c:function-declarator* code codes)
 (list
  code
  (c:parenthesize
   (let ((codes (removeq '() codes)))
    (cond
     ((null? codes) "void")
     ((null? (rest codes)) (first codes))
     (else
      (reduce (lambda (code1 code2) (list code1 "," code2)) codes '())))))))

(define (c:function-declaration p1? p2? p3? code1 code2)
 (list (if p1? (list "static" " ") '())
       (if p2? (list "INLINE" " ") '())
       (if p3? (list "NORETURN" " ") '())
       code1
       " "
       code2
       ";"
       #\newline))

(define (c:specifier-function-declaration p1? p2? p3? v code)
 (if (void? v)
     '()
     (c:function-declaration
      p1? p2? p3? (c:specifier v)
      (if (boxed? v) (c:pointer-declarator code) code))))

(define (c:function-definition p1? p2? p3? code1 code2 code3)
 (list (if p1? (list "static" " ") '())
       (if p2? (list "INLINE" " ") '())
       (if p3? (list "NORETURN" " ") '())
       code1
       " "
       code2
       "{"
       code3
       "}"
       #\newline))

(define (c:specifier-function-definition p1? p2? p3? v code1 code2)
 (if (void? v)
     '()
     (c:function-definition p1? p2? p3? (c:specifier v)
			    (if (boxed? v) (c:pointer-declarator code1) code1)
			    code2)))

(define (c:call* code codes)
 (list
  code
  (c:parenthesize
   (let ((codes (removeq '() codes)))
    (cond
     ((null? codes) '())
     ((null? (rest codes)) (first codes))
     (else
      (reduce (lambda (code1 code2) (list code1 "," code2)) codes '())))))))

(define (c:panic v code)
 (c:call (c:builtin-name "panic" v) (list "\"" code "\"")))

;;; Derived C syntax generators

(define (c:binary-boolean code)
 (lambda (code1 code2)
  (c:conditional (c:binary code1 code code2)
		 (c:call (c:unioner-name (vlad-true) (abstract-boolean)))
		 (c:call (c:unioner-name (vlad-false) (abstract-boolean))))))

(define (c:unary-boolean code)
 (lambda (code1)
  (c:conditional (c:binary code1 code "0.0")
		 (c:call (c:unioner-name (vlad-true) (abstract-boolean)))
		 (c:call (c:unioner-name (vlad-false) (abstract-boolean))))))

(define (c:function-declarator code . codes)
 (c:function-declarator* code codes))

(define (c:call code . codes) (c:call* code codes))

(define (c:slot v code1 code2)
 (if (void? v) 'error (c:binary code1 (if (boxed? v) "->" ".") code2)))

(define (c:new-slot return? v code1 code2)
 (if (null? code1)
     code2
     (c:binary code1 (cond ((boxed? v) "->") (return? ".") (else "_")) code2)))

(define (c:tag v code) (c:slot v code "v"))

(define (c:union v code1 code2)
 ;; The union is always unboxed.
 (c:binary (c:slot v code1 "u") "." code2))

(define (c:dispatch v code codes)
 (assert (and (= (length (get-union-values v)) (length codes))
	      (>= (length (get-union-values v)) 2)))
 ;; This uses per-union tags here instead of per-program tags.
 ;; It would be better to use a switch but while there are conditional
 ;; expressions in C, there are no switch expressions. We could use the GNU C
 ;; statement expression extension. In the case of conditional expressions, we
 ;; could optimize the order (by profiling or static analysis). In the case of
 ;; a switch, could optimize the choice of which case becomes the default.
 (let loop ((codes codes) (i 0))
  (if (null? (rest codes))
      (first codes)
      ;; The type tag is always unboxed.
      (c:conditional (c:binary (c:tag v code) "==" i)
		     (first codes)
		     (loop (rest codes) (+ i 1))))))

(define (c:new-dispatch code codes)
 ;; This uses per-union tags here instead of per-program tags.
 ;; needs work: To optimize the choice of which case becomes the default or to
 ;;             specify that the cases are exhaustive.
 (list "switch"
       (c:parenthesize code)
       "{"
       (map-indexed
	(lambda (code i) (list "case" " " i ":" code "break" ";")) codes)
       "}"))

(define (c:widen v1 v2 code widener-instances)
 (if (abstract-value=? v1 v2)
     code
     (c:call (c:widener-name v1 v2 widener-instances)
	     (if (void? v1) '() code))))

;;; Declaration generators

(define (generate-constructor-declarations vs1-vs2)
 ;; abstraction
 (map (lambda (v)
       (cond
	((void? v) '())
	((union? v)
	 ;; By fortuitous confluence, this will not generate constructor
	 ;; declarations for the empty abstract value.
	 ;; abstraction
	 (map (lambda (u)
	       (c:specifier-function-declaration
		#t #t #f v
		(c:function-declarator (c:unioner-name u v)
				       (c:specifier-parameter u "x"))))
	      (get-union-values v)))
	((abstract-real? v) '())
	(else (c:specifier-function-declaration
	       #t #t #f v
	       (c:function-declarator* (c:constructor-name v)
				       (map c:specifier-parameter
					    (aggregate-value-values v)
					    (generate-slot-names v)))))))
      (append (first vs1-vs2) (second vs1-vs2))))

(define (generate-widener-declaration widener-instance widener-instances p?)
 (let ((v1 (widener-instance-v1 widener-instance))
       (v2 (widener-instance-v2 widener-instance)))
  (c:specifier-function-declaration
   #t p? #f v2
   (c:function-declarator (c:widener-name v1 v2 widener-instances)
			  (c:specifier-parameter v1 "x")))))

(define (generate-widener-declarations widener-instances)
 ;; abstraction
 (append
  ;; abstraction
  (map (lambda (widener-instance)
	(generate-widener-declaration widener-instance widener-instances #t))
       (first widener-instances))
  ;; abstraction
  (map (lambda (widener-instance)
	(generate-widener-declaration widener-instance widener-instances #f))
       (second widener-instances))))

(define (generate-panic-declarations vs1-vs2)
 ;; abstraction
 (map (lambda (v)
       (c:specifier-function-declaration
	#t #t #t v
	(c:function-declarator
	 (c:builtin-name "panic" v)
	 (c:parameter "char" (c:pointer-declarator "x")))))
      (append (first vs1-vs2) (second vs1-vs2))))

(define (real*real-before v)
 (cond ((union? v) (get-union-values v))
       ((vlad-pair? v)
	(cond ((union? (vlad-car v))
	       (map (lambda (u) (vlad-cons u (vlad-cdr v)))
		    (get-union-values (vlad-car v))))
	      ((union? (vlad-cdr v))
	       (map (lambda (u) (vlad-cons (vlad-car v) u))
		    (get-union-values (vlad-cdr v))))
	      ((and (vlad-real? (vlad-car v)) (vlad-real? (vlad-cdr v))) '())
	      (else '())))
       (else '())))

(define (generate-real*real-primitive-declarations s v0 code)
 ;; abstraction
 (map (lambda (v)
       (c:specifier-function-declaration
	#t #t #f v0
	(c:function-declarator (c:builtin-name code v)
			       (c:specifier-parameter v "x"))))
      ;; This is called redundantly twice, once to generate declarations and
      ;; once to generate definitions.
      ;; This topological sort is needed so that all INLINE definitions come
      ;; before their uses as required by gcc.
      (first
       (feedback-topological-sort abstract-value=?
				  real*real-before
				  (lambda (vertex) (internal-error))
				  (map-reduce union-abstract-values
					      '()
					      all-real*real-abstract-subvalues
					      (all-primitives s))))))

(define (real-before v)
 (cond ((union? v) (get-union-values v))
       ((vlad-real? v) '())
       (else '())))

(define (generate-real-primitive-declarations s v0 code)
 ;; abstraction
 (map (lambda (v)
       (c:specifier-function-declaration
	#t #t #f v0
	(c:function-declarator (c:builtin-name code v)
			       (c:specifier-parameter v "x"))))
      ;; This is called redundantly twice, once to generate declarations and
      ;; once to generate definitions.
      ;; This topological sort is needed so that all INLINE definitions come
      ;; before their uses as required by gcc.
      (first
       (feedback-topological-sort abstract-value=?
				  real-before
				  (lambda (vertex) (internal-error))
				  (map-reduce union-abstract-values
					      '()
					      all-real-abstract-subvalues
					      (all-primitives s))))))

(define (generate-type-predicate-declarations s code)
 ;; abstraction
 (map (lambda (v)
       (c:specifier-function-declaration
	#t #t #f (abstract-boolean)
	(c:function-declarator (c:builtin-name code v)
			       (c:specifier-parameter v "x"))))
      (all-primitives s)))

(define (generate-unary-ad-declaration v f code p?)
 (c:specifier-function-declaration
  ;; The call to f might issue "might" warnings and might return an empty
  ;; abstract value.
  #t p? #f (f v)
  (c:function-declarator (c:builtin-name code v)
			 (c:specifier-parameter v "x"))))

(define (generate-unary-ad-declarations s descend? f code)
 ;; here I am: The result of f might violate the syntactic constraints.
 (let ((vs1a-vs2a (all-sorted-unary-ad s descend?)))
  ;; abstraction
  (append
   ;; abstraction
   (map (lambda (v) (generate-unary-ad-declaration v f code #t))
	(first vs1a-vs2a))
   ;; abstraction
   (map (lambda (v) (generate-unary-ad-declaration v f code #f))
	(second vs1a-vs2a)))))

(define (generate-binary-ad-declaration v g code p?)
 (c:specifier-function-declaration
  ;; The call to g might issue "might" warnings and might return an empty
  ;; abstract value.
  #t p? #f (g v)
  (c:function-declarator (c:builtin-name code v)
			 (c:specifier-parameter v "x"))))

(define (generate-binary-ad-declarations
	 s descend? f? f f-inverse g aggregates-match? before code)
 ;; here I am: The results of f, f-inverse, and g might violate the
 ;;            syntactic constraints.
 (let ((vs1a-vs2a (all-sorted-binary-ad
		   s descend? f? f f-inverse aggregates-match? before)))
  ;; abstraction
  (append
   ;; abstraction
   (map (lambda (v) (generate-binary-ad-declaration v g code #t))
	(first vs1a-vs2a))
   ;; abstraction
   (map (lambda (v) (generate-binary-ad-declaration v g code #f))
	(second vs1a-vs2a)))))

(define (generate-if-and-function-declaration instance function-instances p?)
 (cond ((if-instance? instance)
	(let ((v (if-instance-v instance)))
	 (c:specifier-function-declaration
	  #t #t #f (if-return-value v)
	  (c:function-declarator (c:builtin-name "if_procedure" v)
				 (c:specifier-parameter v "x")))))
       ((function-instance? instance)
	(let ((v1 (function-instance-v1 instance))
	      (v2 (function-instance-v2 instance)))
	 (c:specifier-function-declaration
	  #t p? #f (abstract-apply v1 v2)
	  (c:function-declarator (c:function-name v1 v2 function-instances)
				 (c:specifier-parameter v1 "c")
				 (c:specifier-parameter v2 "x")))))
       (else (internal-error))))

(define (generate-if-and-function-declarations
	 function-instances instances1-instances2)
 ;; abstraction
 (append
  ;; abstraction
  (map (lambda (instance)
	(generate-if-and-function-declaration instance function-instances #t))
       (first instances1-instances2))
  ;; abstraction
  (map (lambda (instance)
	(generate-if-and-function-declaration instance function-instances #f))
       (second instances1-instances2))))

;;; Expression generators

(define (generate-destructure v0 p v code k)
 (let outer ((p p) (v v) (code code) (alist '()) (codes '()) (k k))
  (cond
   ;; This case comes first to avoid the dispatch.
   ((variable-access-expression? p)
    (k (cons (cons (variable-access-expression-variable p) v) alist)
       ;; abstraction
       (append
	codes
	(list
	 (c:specifier-init-declaration
	  v (c:variable-name (variable-access-expression-variable p)) code)))))
   ((union? v)
    (c:dispatch v
		code
		(map (lambda (code1 u)
		      (c:statement-expression
		       (outer p u (c:union v code code1) alist codes k)))
		     (generate-slot-names v)
		     (get-union-values v))))
   ((constant-expression? p)
    ;; needs work: To generate run-time equivalence check when the constant
    ;;             expression parameter and/or argument contain abstract
    ;;             booleans or abstract reals. When we do so, we need to call
    ;;             c:widen appropriately. These would correspond to the calls A
    ;;             to widen-abstract-value in abstract-destructure.
    (if (abstract-value-nondisjoint?
	 (concrete-value->abstract-value (constant-expression-value p)) v)
	(k alist codes)
	(c:panic v0 (format #f "Argument is not an equivalent value for ~s"
			    (externalize-expression p)))))
   ((lambda-expression? p)
    (if (and (nonrecursive-closure? v)
	     (dereferenced-expression-eqv?
	      p (nonrecursive-closure-lambda-expression v)))
	(let inner ((xs1 (parameter-variables p))
		    (xs2 (nonrecursive-closure-variables v))
		    (vs (get-nonrecursive-closure-values v))
		    (alist alist)
		    (codes codes)
		    (k k))
	 (if (null? xs1)
	     (k alist codes)
	     (outer (new-variable-access-expression (first xs1))
		    (first vs)
		    (c:slot v code (c:variable-name (first xs2)))
		    alist
		    codes
		    (lambda (alist codes)
		     (inner (rest xs1) (rest xs2) (rest vs) alist codes k)))))
	(c:panic
	 v0 (format #f "Argument is not a matching nonrecursive closure for ~s"
		    (externalize-expression p)))))
   ((letrec-expression? p)
    (assert (and (variable-access-expression? (letrec-expression-body p))
		 (memp variable=?
		       (variable-access-expression-variable
			(letrec-expression-body p))
		       (letrec-expression-procedure-variables p))))
    (if (and (recursive-closure? v)
	     (= (recursive-closure-index v)
		(positionp variable=?
			   (variable-access-expression-variable
			    (letrec-expression-body p))
			   (letrec-expression-procedure-variables p)))
	     (= (vector-length
		 (recursive-closure-procedure-variables v))
		(length (letrec-expression-procedure-variables p)))
	     (= (vector-length
		 (recursive-closure-lambda-expressions v))
		(length (letrec-expression-lambda-expressions p)))
	     (every dereferenced-expression-eqv?
		    (vector->list (recursive-closure-lambda-expressions v))
		    (letrec-expression-lambda-expressions p)))
	(let inner ((xs1 (parameter-variables p))
		    (xs2 (recursive-closure-variables v))
		    (vs (get-recursive-closure-values v))
		    (alist alist)
		    (codes codes)
		    (k k))
	 (if (null? xs1)
	     (k alist codes)
	     (outer (new-variable-access-expression (first xs1))
		    (first vs)
		    (c:slot v code (c:variable-name (first xs2)))
		    alist
		    codes
		    (lambda (alist codes)
		     (inner (rest xs1) (rest xs2) (rest vs) alist codes k)))))
	(c:panic
	 v0 (format #f "Argument is not a matching recursive closure for ~s"
		    (externalize-expression p)))))
   ((cons-expression? p)
    (if (and (tagged-pair? v)
	     (equal-tags? (cons-expression-tags p) (tagged-pair-tags v)))
	(outer (cons-expression-car p)
	       (get-tagged-pair-car v)
	       (c:slot v code "a")
	       alist
	       codes
	       (lambda (alist codes)
		(outer (cons-expression-cdr p)
		       (get-tagged-pair-cdr v)
		       (c:slot v code "d")
		       alist
		       codes
		       k)))
	(c:panic
	 v0 (format #f "Argument is not a matching tagged pair with tags ~s"
		    (cons-expression-tags p)))))
   (else (internal-error)))))

(define (generate-reference v x xs xs2)
 (cond ((memp variable=? x xs2) "c")
       ((memp variable=? x xs) (c:slot v "c" (c:variable-name x)))
       (else (c:variable-name x))))

(define (generate-call
	 v0 v1 v2 code1 code2 function-instances widener-instances)
 ;; needs work: We don't check the "Argument has wrong type for target"
 ;;             condition.
 ;; This corresponds to call B to widen-abstract-value in abstract-eval!.
 (c:widen
  (abstract-apply v1 v2)
  v0
  (cond
   ((union? v1)
    (c:let
     v1
     "y"
     code1
     (c:dispatch
      v1
      "y"
      (map
       (lambda (code1 u1)
	(c:widen
	 (abstract-apply u1 v2)
	 (abstract-apply v1 v2)
	 (cond
	  ((primitive-procedure? u1)
	   (c:call ((primitive-procedure-generator u1) v2) code2))
	  ((closure? u1)
	   (c:call (c:function-name u1 v2 function-instances)
		   ;; here I am: widen?
		   (if (void? u1) '() (c:union v1 "y" code1))
		   ;; here I am: widen?
		   code2))
	  (else (c:panic (abstract-apply u1 v2) "Target is not a procedure")))
	 widener-instances))
       (generate-slot-names v1)
       (get-union-values v1)))))
   ((primitive-procedure? v1)
    (c:call ((primitive-procedure-generator v1) v2) code2))
   ((closure? v1)
    (c:call (c:function-name v1 v2 function-instances)
	    ;; here I am: widen?
	    code1
	    ;; here I am: widen?
	    code2))
   (else (c:panic (abstract-apply v1 v2) "Target is not a procedure")))
  widener-instances))

(define (generate-expression
	 e vs v0 xs xs2 bs function-instances widener-instances)
 ;; xs is the list of free variables of the environent in which e is evaluated.
 ;; xs2 is the list of procedure variables of the environent in which e is
 ;;     evaluated.
 (cond
  ((empty-abstract-value? (abstract-eval1 e vs))
   (c:panic (abstract-eval1 e vs)
	    "A run-time error that was detected at compile time has occurred"))
  ((constant-expression? e)
   (assert (void? (constant-expression-value e)))
   ;; This c:widen is necessary for the case where the constant expression
   ;; value is an inexact real and *imprecise-inexacts?* is true. It also is
   ;; necessary for the case where the constant expression value is widened
   ;; because it violates the syntactic constraints (presumably tagged pair
   ;; depth limit). This corresponds to call E to widen-abstract-value in
   ;; abstract-eval-prime!.
   (c:widen
    (constant-expression-value e) (abstract-eval1 e vs) '() widener-instances))
  ((variable-access-expression? e)
   ;; There does not need to be a call to c:widen to correspond to call F to
   ;; widen-abstract-value in abstract-eval-prime!.
   (generate-reference v0 (variable-access-expression-variable e) xs xs2))
  ((lambda-expression? e)
   ;; This c:widen is necessary for the case where the closure created violates
   ;; the syntactic constraints (presumably closure depth limit or
   ;; backpropagator depth limit). This corresponds to call G to
   ;; widen-abstract-value in abstract-eval-prime!.
   (c:widen
    (new-nonrecursive-closure vs e)
    (abstract-eval1 e vs)
    (c:call* (c:constructor-name (new-nonrecursive-closure vs e))
	     (map (lambda (x1 v1)
		   (if (void? v1) '() (generate-reference v0 x1 xs xs2)))
		  (free-variables e)
		  vs))
    widener-instances))
  ((application? e)
   (if (lambda-expression? (application-callee e))
       ;; This handling of LET is an optimization.
       ;; needs work: We don't check the "Argument has wrong type for target"
       ;;             condition.
       (let ((e1 (lambda-expression-body (application-callee e)))
	     (p (lambda-expression-parameter (application-callee e)))
	     (tags1 (lambda-expression-tags (application-callee e)))
	     (v (abstract-eval1
		 (application-argument e)
		 (restrict-environment vs (application-argument-indices e)))))
	(c:let
	 v
	 "z"
	 ;; here I am: widen?
	 (if (void? v)
	     '()
	     (generate-expression
	      (application-argument e)
	      (restrict-environment vs (application-argument-indices e))
	      v0
	      xs
	      xs2
	      bs
	      function-instances
	      widener-instances))
	 (generate-destructure
	  (abstract-eval1 e vs)
	  p
	  v
	  "z"
	  (lambda (alist codes)
	   (let ((vs1 (construct-environment-for-let e vs alist)))
	    (c:statement-expression
	     codes
	     ;; This corresponds to call B-prime to widen-abstract-value in
	     ;; abstract-eval!.
	     (c:widen
	      (abstract-eval1 e1 vs1)
	      (abstract-eval1 e vs)
	      (generate-expression
	       e1 vs1 v0 xs xs2 bs function-instances widener-instances)
	      widener-instances)))))))
       (let ((v1 (abstract-eval1
		  (application-callee e)
		  (restrict-environment vs (application-callee-indices e))))
	     (v2 (abstract-eval1
		  (application-argument e)
		  (restrict-environment vs (application-argument-indices e)))))
	(generate-call
	 (abstract-eval1 e vs)
	 v1
	 v2
	 (if (void? v1)
	     '()
	     (generate-expression
	      (application-callee e)
	      (restrict-environment vs (application-callee-indices e))
	      v0
	      xs
	      xs2
	      bs
	      function-instances
	      widener-instances))
	 (if (void? v2)
	     '()
	     (generate-expression
	      (application-argument e)
	      (restrict-environment vs (application-argument-indices e))
	      v0
	      xs
	      xs2
	      bs
	      function-instances
	      widener-instances))
	 function-instances
	 widener-instances))))
  ((letrec-expression? e)
   ;; This corresponds to call C to widen-abstract-value in abstract-eval!.
   (c:widen (abstract-eval1 (letrec-expression-body e)
			    (letrec-nested-environment vs e))
	    (abstract-eval1 e vs)
	    (generate-expression
	     (letrec-expression-body e)
	     (map widen-abstract-value (letrec-nested-environment vs e))
	     v0
	     xs
	     xs2
	     bs
	     function-instances
	     widener-instances)
	    widener-instances))
  ((cons-expression? e)
   (let ((v1 (abstract-eval1
	      (cons-expression-car e)
	      (restrict-environment vs (cons-expression-car-indices e))))
	 (v2 (abstract-eval1
	      (cons-expression-cdr e)
	      (restrict-environment vs (cons-expression-cdr-indices e)))))
    ;; needs work: We don't check the "Argument has wrong type for target"
    ;;             condition.
    ;; This c:widen is necessary for the case where the tagged pair created
    ;; violates the syntactic constraints (presumably tagged pair depth limit)
    ;; or where the flow analysis widened due to imprecision. This corresponds
    ;; to calls D to widen-abstract-value in abstract-eval!.
    (c:widen
     (new-tagged-pair (cons-expression-tags e) v1 v2)
     (abstract-eval1 e vs)
     (c:call
      (c:constructor-name (new-tagged-pair (cons-expression-tags e) v1 v2))
      (if (void? v1)
	  '()
	  (generate-expression
	   (cons-expression-car e)
	   (restrict-environment vs (cons-expression-car-indices e))
	   v0
	   xs
	   xs2
	   bs
	   function-instances
	   widener-instances))
      (if (void? v2)
	  '()
	  (generate-expression
	   (cons-expression-cdr e)
	   (restrict-environment vs (cons-expression-cdr-indices e))
	   v0
	   xs
	   xs2
	   bs
	   function-instances
	   widener-instances)))
     widener-instances)))
  (else (internal-error))))

(define (generate-letrec-bindings e vs xs xs2 widener-instances)
 (cond
  ((constant-expression? e) '())
  ((variable-access-expression? e) '())
  ((lambda-expression? e) '())
  ((application? e)
   ;; abstraction
   (list
    (generate-letrec-bindings
     (application-callee e)
     (restrict-environment vs (application-callee-indices e))
     xs
     xs2
     widener-instances)
    (generate-letrec-bindings
     (application-argument e)
     (restrict-environment vs (application-argument-indices e))
     xs
     xs2
     widener-instances)))
  ((letrec-expression? e)
   ;; abstraction
   (list
    ;; abstraction
    (map (lambda (x)
	  (let* ((v (new-recursive-closure
		     (restrict-environment vs (letrec-expression-indices e))
		     (list->vector (letrec-expression-procedure-variables e))
		     (list->vector (letrec-expression-lambda-expressions e))
		     (positionp
		      variable=? x (letrec-expression-procedure-variables e))))
		 (v0 (widen-abstract-value v)))
	   (c:specifier-init-declaration
	    v0
	    (c:variable-name x)
	    ;; This c:widen is necessary for the case where the closure created
	    ;; violates the syntactic constraints (presumably closure depth
	    ;; limit or backpropagator depth limit).
	    (c:widen
	     v
	     v0
	     (c:call*
	      (c:constructor-name v)
	      (map (lambda (x1 v1)
		    (if (void? v1) '() (generate-reference v x1 xs xs2)))
		   (letrec-expression-variables e)
		   (restrict-environment vs (letrec-expression-indices e))))
	     widener-instances))))
	 (letrec-expression-procedure-variables e))
    (generate-letrec-bindings
     (letrec-expression-body e)
     (map widen-abstract-value (letrec-nested-environment vs e))
     xs
     xs2
     widener-instances)))
  ((cons-expression? e)
   ;; abstraction
   (list (generate-letrec-bindings
	  (cons-expression-car e)
	  (restrict-environment vs (cons-expression-car-indices e))
	  xs
	  xs2
	  widener-instances)
	 (generate-letrec-bindings
	  (cons-expression-cdr e)
	  (restrict-environment vs (cons-expression-cdr-indices e))
	  xs
	  xs2
	  widener-instances)))
  (else (internal-error))))

;;; Definition generators

(define (generate-constructor-definitions vs1-vs2)
 ;; abstraction
 (map
  (lambda (v)
   (cond
    ((void? v) '())
    ((union? v)
     ;; By fortuitous confluence, this will not generate constructor
     ;; definitions for the empty abstract value.
     ;; abstraction
     (map (lambda (u)
	   (c:specifier-function-definition
	    #t #t #f v
	    (c:function-declarator (c:unioner-name u v)
				   (c:specifier-parameter u "x"))
	    ;; abstraction
	    (list (if (boxed? v)
		      (c:specifier-init-declaration
		       v
		       "r"
		       (c:pointer-cast
			(c:specifier v)
			;; We don't check for out of memory.
			(c:call "GC_malloc" (c:sizeof (c:specifier v)))))
		      (c:specifier-declaration v "r"))
		  (c:assignment
		   ;; The type tag is always unboxed.
		   (c:tag v "r")
		   ;; This uses per-union tags here instead of per-program
		   ;; tags.
		   (positionp abstract-value=? u (get-union-values v)))
		  (if (void? u)
		      '()
		      (c:assignment (c:union v
					     "r"
					     ;; abstraction
					     (list "s" (c:index u)))
				    "x"))
		  (c:return "r"))))
	  (get-union-values v)))
    ((abstract-real? v) '())
    (else
     (c:specifier-function-definition
      #t #t #f v
      (c:function-declarator* (c:constructor-name v)
			      (map c:specifier-parameter
				   (aggregate-value-values v)
				   (generate-slot-names v)))
      ;; abstraction
      (list (if (boxed? v)
		(c:specifier-init-declaration
		 v
		 "r"
		 (c:pointer-cast
		  (c:specifier v)
		  ;; We don't check for out of memory.
		  (c:call "GC_malloc" (c:sizeof (c:specifier v)))))
		(c:specifier-declaration v "r"))
	    (map
	     (lambda (code1 v1)
	      (if (void? v1) '() (c:assignment (c:slot v "r" code1) code1)))
	     (generate-slot-names v)
	     (aggregate-value-values v))
	    (c:return "r"))))))
  (append (first vs1-vs2) (second vs1-vs2))))

(define (generate-widener-definition widener-instance widener-instances p?)
 (let ((v1 (widener-instance-v1 widener-instance))
       (v2 (widener-instance-v2 widener-instance)))
  (c:specifier-function-definition
   #t p? #f v2
   (c:function-declarator (c:widener-name v1 v2 widener-instances)
			  (c:specifier-parameter v1 "x"))
   (if (empty-abstract-value? v1)
       ;; abstraction
       (list (c:specifier-declaration v2 "r") (c:return "r"))
       (c:return
	(cond
	 ;; See the note for this case in all-subwidener-instances.
	 ((union? v1)
	  (c:dispatch
	   v1
	   "x"
	   (map (lambda (code1 u1)
		 (c:widen u1 v2 (c:union v1 "x" code1) widener-instances))
		(generate-slot-names v1)
		(get-union-values v1))))
	 ;; See the notes for this case in all-subwidener-instances.
	 ((union? v2)
	  (let ((u2 (find-if (lambda (u2) (abstract-value-subset? v1 u2))
			     (get-union-values v2))))
	   (c:call (c:unioner-name u2 v2)
		   (if (void? u2) '() (c:widen v1 u2 "x" widener-instances)))))
	 ((scalar-value? v2)
	  (cond ((void? v2) 'error)
		;; This assumes that Scheme inexact numbers are printed as C
		;; doubles.
		((real? v1) (exact->inexact v1))
		(else (internal-error))))
	 (else
	  (c:call*
	   (c:constructor-name v2)
	   ;; This will only be done on conforming structures since the
	   ;; analysis is almost union free.
	   (map
	    (lambda (code1a v1a v2a)
	     (if (void? v2a)
		 '()
		 (c:widen v1a v2a (c:slot v1 "x" code1a) widener-instances)))
	    (generate-slot-names v1)
	    (aggregate-value-values v1)
	    (aggregate-value-values v2))))))))))

(define (generate-widener-definitions widener-instances)
 ;; abstraction
 (append
  ;; abstraction
  (map (lambda (widener-instance)
	(generate-widener-definition widener-instance widener-instances #t))
       (first widener-instances))
  ;; abstraction
  (map (lambda (widener-instance)
	(generate-widener-definition widener-instance widener-instances #f))
       (second widener-instances))))

(define (generate-panic-definitions vs1-vs2)
 ;; abstraction
 (map (lambda (v)
       (c:specifier-function-definition
	#t #t #t v
	(c:function-declarator (c:builtin-name "panic" v)
			       (c:parameter "char" (c:pointer-declarator "x")))
	;; abstraction
	"fputs(x,stderr);fputc('\\n',stderr);exit(EXIT_FAILURE);"))
      (append (first vs1-vs2) (second vs1-vs2))))

(define (generate-real*real-primitive-definitions s v0 code1 code2 generate)
 ;; abstraction
 (map
  (lambda (v)
   (c:specifier-function-definition
    #t #t #f v0
    (c:function-declarator (c:builtin-name code1 v)
			   (c:specifier-parameter v "x"))
    (c:return
     (cond
      ((union? v)
       (c:dispatch v
		   "x"
		   (map (lambda (code u)
			 (c:call (c:builtin-name code1 u)
				 (if (void? u) '() (c:union v "x" code))))
			(generate-slot-names v)
			(get-union-values v))))
      ((vlad-pair? v)
       (let ((v1 (vlad-car v)) (v2 (vlad-cdr v)))
	(cond
	 ((union? v1)
	  (c:dispatch
	   v1
	   (c:slot v "x" "a")
	   (map
	    (lambda (code u1)
	     (c:call
	      (c:builtin-name code1 (vlad-cons u1 v2))
	      (if (void? (vlad-cons u1 v2))
		  '()
		  (c:call
		   (c:constructor-name (vlad-cons u1 v2))
		   (if (void? u1) '() (c:union v1 (c:slot v "x" "a") code))
		   (if (void? v2) '() (c:slot v "x" "d"))))))
	    (generate-slot-names v1)
	    (get-union-values v1))))
	 ((union? v2)
	  (c:dispatch
	   v2
	   (c:slot v "x" "d")
	   (map
	    (lambda (code u2)
	     (c:call
	      (c:builtin-name code1 (vlad-cons v1 u2))
	      (if (void? (vlad-cons v1 u2))
		  '()
		  (c:call
		   (c:constructor-name (vlad-cons v1 u2))
		   (if (void? v1) '() (c:slot v "x" "a"))
		   (if (void? u2) '() (c:union v2 (c:slot v "x" "d") code))))))
	    (generate-slot-names v2)
	    (get-union-values v2))))
	 ((and (vlad-real? v1) (vlad-real? v2))
	  (generate
	   ;; This assumes that Scheme inexact numbers are printed as C
	   ;; doubles.
	   (if (void? v1) (exact->inexact v1) (c:slot v "x" "a"))
	   ;; This assumes that Scheme inexact numbers are printed as C
	   ;; doubles.
	   (if (void? v2) (exact->inexact v2) (c:slot v "x" "d"))))
	 (else (c:panic v0 (format #f "Argument to ~a is invalid" code2))))))
      (else (c:panic v0 (format #f "Argument to ~a is invalid" code2)))))))
  ;; This is called redundantly twice, once to generate declarations and once
  ;; to generate definitions.
  ;; This topological sort is needed so that all INLINE definitions come before
  ;; their uses as required by gcc.
  (first
   (feedback-topological-sort abstract-value=?
			      real*real-before
			      (lambda (vertex) (internal-error))
			      (map-reduce union-abstract-values
					  '()
					  all-real*real-abstract-subvalues
					  (all-primitives s))))))

(define (generate-real-primitive-definitions s v0 code1 code2 generate)
 ;; abstraction
 (map (lambda (v)
       (c:specifier-function-definition
	#t #t #f v0
	(c:function-declarator (c:builtin-name code1 v)
			       (c:specifier-parameter v "x"))
	(c:return
	 (cond
	  ((union? v)
	   (c:dispatch v
		       "x"
		       (map (lambda (code u)
			     (c:call (c:builtin-name code1 u)
				     (if (void? u) '() (c:union v "x" code))))
			    (generate-slot-names v)
			    (get-union-values v))))
	  ;; This assumes that Scheme inexact numbers are printed as C doubles.
	  ((vlad-real? v) (generate (if (void? v) (exact->inexact v) "x")))
	  (else (c:panic v0 (format #f "Argument to ~a is invalid" code2)))))))
      ;; This is called redundantly twice, once to generate declarations and
      ;; once to generate definitions.
      ;; This topological sort is needed so that all INLINE definitions come
      ;; before their uses as required by gcc.
      (first
       (feedback-topological-sort abstract-value=?
				  real-before
				  (lambda (vertex) (internal-error))
				  (map-reduce union-abstract-values
					      '()
					      all-real-abstract-subvalues
					      (all-primitives s))))))

(define (generate-type-predicate-definitions s code p?)
 ;; abstraction
 (map (lambda (v)
       (unless (union? v) (internal-error))
       (c:specifier-function-definition
	#t #t #f (abstract-boolean)
	(c:function-declarator (c:builtin-name code v)
			       (c:specifier-parameter v "x"))
	(c:return
	 (c:dispatch
	  v
	  "x"
	  (map (lambda (code u)
		(c:call (c:unioner-name (if (p? u) (vlad-true) (vlad-false))
					(abstract-boolean))))
	       (generate-slot-names v)
	       (get-union-values v))))))
      (all-primitives s)))

(define (generate-unary-ad-definition v f code generate p?)
 (c:specifier-function-definition
  ;; The call to f might issue "might" warnings and might return an empty
  ;; abstract value.
  #t p? #f (f v)
  (c:function-declarator (c:builtin-name code v) (c:specifier-parameter v "x"))
  (c:return (generate v))))

(define (generate-unary-ad-definitions s descend? f code generate)
 ;; here I am: The result of f might violate the syntactic constraints.
 (let ((vs1a-vs2a (all-sorted-unary-ad s descend?)))
  ;; abstraction
  (append
   ;; abstraction
   (map (lambda (v) (generate-unary-ad-definition v f code generate #t))
	(first vs1a-vs2a))
   ;; abstraction
   (map (lambda (v) (generate-unary-ad-definition v f code generate #f))
	(second vs1a-vs2a)))))

(define (generate-binary-ad-definition v g code generate p?)
 (c:specifier-function-definition
  ;; The call to g might issue "might" warnings and might return an empty
  ;; abstract value.
  #t p? #f (g v)
  (c:function-declarator (c:builtin-name code v) (c:specifier-parameter v "x"))
  (c:return (generate v))))

(define (generate-binary-ad-definitions
	 s descend? f? f f-inverse g aggregates-match? before code generate)
 ;; here I am: The results of f, f-inverse, and g might violate the syntactic
 ;;            constraints.
 (let ((vs1a-vs2a (all-sorted-binary-ad
		   s descend? f? f f-inverse aggregates-match? before)))
  ;; abstraction
  (append
   ;; abstraction
   (map (lambda (v) (generate-binary-ad-definition v g code generate #t))
	(first vs1a-vs2a))
   ;; abstraction
   (map (lambda (v) (generate-binary-ad-definition v g code generate #f))
	(second vs1a-vs2a)))))

(define (generate-zero-definitions widener-instances)
 (generate-unary-ad-definitions
  'zero (lambda (v) #t) zero "zero"
  (lambda (v)
   (cond
    ((union? v)
     (c:dispatch
      v
      "x"
      (map (lambda (code u)
	    (c:widen (zero u)
		     (zero v)
		     (c:call (c:builtin-name "zero" u)
			     (if (void? u) '() (c:union v "x" code)))
		     widener-instances))
	   (generate-slot-names v)
	   (get-union-values v))))
    ((or (nonrecursive-closure? v)
	 (recursive-closure? v)
	 (perturbation-tagged-value? v)
	 (bundle? v)
	 (sensitivity-tagged-value? v)
	 (reverse-tagged-value? v)
	 (tagged-pair? v))
     (c:call* (c:constructor-name (zero v))
	      (map (lambda (code1 v1)
		    (if (void? (zero v1))
			'()
			(c:call (c:builtin-name "zero" v1)
				(if (void? v1) '() (c:slot v "x" code1)))))
		   (generate-slot-names v)
		   (aggregate-value-values v))))
    (else 'error)))))

(define (generate-perturb-definitions widener-instances)
 (generate-unary-ad-definitions
  'perturb
  (lambda (v)
   (and (not (perturbation-tagged-value? v))
	(not (bundle? v))
	(not (sensitivity-tagged-value? v))
	(not (reverse-tagged-value? v))))
  perturb "perturb"
  (lambda (v)
   (cond
    ((union? v)
     (c:dispatch
      v
      "x"
      (map (lambda (code u)
	    (c:widen (perturb u)
		     (perturb v)
		     (c:call (c:builtin-name "perturb" u)
			     (if (void? u) '() (c:union v "x" code)))
		     widener-instances))
	   (generate-slot-names v)
	   (get-union-values v))))
    ((or (vlad-real? v)
	 (perturbation-tagged-value? v)
	 (bundle? v)
	 (sensitivity-tagged-value? v)
	 (reverse-tagged-value? v))
     (c:call (c:constructor-name (perturb v)) (if (void? v) '() "x")))
    ((or (nonrecursive-closure? v) (recursive-closure? v) (tagged-pair? v))
     (c:call* (c:constructor-name (perturb v))
	      (map (lambda (code1 v1)
		    (if (void? (perturb v1))
			'()
			(c:call (c:builtin-name "perturb" v1)
				(if (void? v1) '() (c:slot v "x" code1)))))
		   (generate-slot-names v)
		   (aggregate-value-values v))))
    (else 'error)))))

(define (generate-unperturb-definitions widener-instances)
 (generate-unary-ad-definitions
  'unperturb
  (lambda (v)
   (and (perturbation-value? v) (not (perturbation-tagged-value? v))))
  unperturb "unperturb"
  (lambda (v)
   (cond
    ((union? v)
     (c:dispatch
      v
      "x"
      (map (lambda (code u)
	    (c:widen (unperturb u)
		     (unperturb v)
		     (c:call (c:builtin-name "unperturb" u)
			     (if (void? u) '() (c:union v "x" code)))
		     widener-instances))
	   (generate-slot-names v)
	   (get-union-values v))))
    ((perturbation-tagged-value? v) (c:slot v "x" "p"))
    ((and
      (or (nonrecursive-closure? v) (recursive-closure? v) (tagged-pair? v))
      (not (empty-abstract-value? (unperturb v))))
     (c:call* (c:constructor-name (unperturb v))
	      (map (lambda (code1 v1)
		    (if (void? (unperturb v1))
			'()
			(c:call (c:builtin-name "unperturb" v1)
				(if (void? v1) '() (c:slot v "x" code1)))))
		   (generate-slot-names v)
		   (aggregate-value-values v))))
    (else (c:panic (unperturb v)
		   "Argument to unperturb is not a perturbation value"))))))

(define (generate-primal-definitions widener-instances)
 (generate-unary-ad-definitions
  'primal (lambda (v) (and (forward-value? v) (not (bundle? v))))
  primal "primal"
  (lambda (v)
   (cond
    ((union? v)
     (c:dispatch
      v
      "x"
      (map (lambda (code u)
	    (c:widen (primal u)
		     (primal v)
		     (c:call (c:builtin-name "primal" u)
			     (if (void? u) '() (c:union v "x" code)))
		     widener-instances))
	   (generate-slot-names v)
	   (get-union-values v))))
    ((bundle? v) (c:slot v "x" "p"))
    ((and
      (or (nonrecursive-closure? v) (recursive-closure? v) (tagged-pair? v))
      (not (empty-abstract-value? (primal v))))
     (c:call* (c:constructor-name (primal v))
	      (map (lambda (code1 v1)
		    (if (void? (primal v1))
			'()
			(c:call (c:builtin-name "primal" v1)
				(if (void? v1) '() (c:slot v "x" code1)))))
		   (generate-slot-names v)
		   (aggregate-value-values v))))
    (else (c:panic (primal v) "Argument to primal is not a forward value"))))))

(define (generate-tangent-definitions widener-instances)
 (generate-unary-ad-definitions
  'tangent (lambda (v) (and (forward-value? v) (not (bundle? v))))
  tangent "tangent"
  (lambda (v)
   (cond
    ((union? v)
     (c:dispatch
      v
      "x"
      (map (lambda (code u)
	    (c:widen (tangent u)
		     (tangent v)
		     (c:call (c:builtin-name "tangent" u)
			     (if (void? u) '() (c:union v "x" code)))
		     widener-instances))
	   (generate-slot-names v)
	   (get-union-values v))))
    ((bundle? v) (c:slot v "x" "v"))
    ((and
      (or (nonrecursive-closure? v) (recursive-closure? v) (tagged-pair? v))
      (not (empty-abstract-value? (tangent v))))
     (c:call* (c:constructor-name (tangent v))
	      (map (lambda (code1 v1)
		    (if (void? (tangent v1))
			'()
			(c:call (c:builtin-name "tangent" v1)
				(if (void? v1) '() (c:slot v "x" code1)))))
		   (generate-slot-names v)
		   (aggregate-value-values v))))
    (else
     (c:panic (tangent v) "Argument to tangent is not a forward value"))))))

(define (bundle-before v)
 (cond
  ((union? v) (get-union-values v))
  ((vlad-pair? v)
   (let ((v1 (vlad-car v)) (v2 (vlad-cdr v)))
    (cond
     ((union? v1) (map (lambda (u1) (vlad-cons u1 v2)) (get-union-values v1)))
     ((union? v2) (map (lambda (u2) (vlad-cons v1 u2)) (get-union-values v2)))
     ((and (perturbation-tagged-value? v2) (union? (unperturb v2)))
      (map (lambda (u2) (vlad-cons v1 (perturb u2)))
	   (get-union-values (unperturb v2))))
     ((and (or (vlad-empty-list? v1)
	       (vlad-true? v1)
	       (vlad-false? v1)
	       (primitive-procedure? v1))
	   (every-bundlable? v1 v2))
      '())
     ((or (and (vlad-real? v1) (every-bundlable? v1 v2))
	  (and (perturbation-tagged-value? v1) (some-bundlable? v1 v2))
	  (and (bundle? v1) (some-bundlable? v1 v2))
	  (and (sensitivity-tagged-value? v1) (some-bundlable? v1 v2))
	  (and (reverse-tagged-value? v1) (some-bundlable? v1 v2)))
      '())
     ((bundle-aggregates-match? v1 v2)
      (map vlad-cons (aggregate-value-values v1) (aggregate-value-values v2)))
     (else '()))))
  (else '())))

(define (generate-bundle-definitions widener-instances)
 (generate-binary-ad-definitions
  'bundle
  (lambda (v)
   (and (not (perturbation-tagged-value? v))
	(not (bundle? v))
	(not (sensitivity-tagged-value? v))
	(not (reverse-tagged-value? v))))
  perturbation-tagged-value? perturb unperturb bundle
  bundle-aggregates-match? bundle-before "bundle"
  (lambda (v)
   (cond
    ((union? v)
     (c:dispatch
      v
      "x"
      (map (lambda (code u)
	    (c:widen (bundle u)
		     (bundle v)
		     (c:call (c:builtin-name "bundle" u)
			     (if (void? u) '() (c:union v "x" code)))
		     widener-instances))
	   (generate-slot-names v)
	   (get-union-values v))))
    ((vlad-pair? v)
     (let ((v1 (vlad-car v)) (v2 (vlad-cdr v)))
      (cond
       ((union? v1)
	(c:dispatch
	 v1
	 (c:slot v "x" "a")
	 (map (lambda (code1 u1)
	       (c:widen
		(bundle (vlad-cons u1 v2))
		(bundle (vlad-cons v1 v2))
		(c:call (c:builtin-name "bundle" (vlad-cons u1 v2))
			(if (void? (vlad-cons u1 v2))
			    '()
			    (c:call (c:constructor-name (vlad-cons u1 v2))
				    (if (void? u1)
					'()
					(c:union v1 (c:slot v "x" "a") code1))
				    (if (void? v2) '() (c:slot v "x" "d")))))
		widener-instances))
	      (generate-slot-names v1)
	      (get-union-values v1))))
       ((union? v2)
	(c:dispatch
	 v2
	 (c:slot v "x" "d")
	 (map
	  (lambda (code2 u2)
	   (c:widen
	    (bundle (vlad-cons v1 u2))
	    (bundle (vlad-cons v1 v2))
	    (c:call (c:builtin-name "bundle" (vlad-cons v1 u2))
		    (if (void? (vlad-cons v1 u2))
			'()
			(c:call (c:constructor-name (vlad-cons v1 u2))
				(if (void? v1) '() (c:slot v "x" "a"))
				(if (void? u2)
				    '()
				    (c:union v2 (c:slot v "x" "d") code2)))))
	    widener-instances))
	  (generate-slot-names v2)
	  (get-union-values v2))))
       ((and (perturbation-tagged-value? v2) (union? (unperturb v2)))
	(c:dispatch
	 (unperturb v2)
	 (c:slot v2 (c:slot v "x" "d") "p")
	 (map
	  (lambda (code2 u2)
	   (c:widen
	    (bundle (vlad-cons v1 (perturb u2)))
	    (bundle (vlad-cons v1 v2))
	    (c:call
	     (c:builtin-name "bundle" (vlad-cons v1 (perturb u2)))
	     (if (void? (vlad-cons v1 (perturb u2)))
		 '()
		 (c:call
		  (c:constructor-name (vlad-cons v1 (perturb u2)))
		  (if (void? v1) '() (c:slot v "x" "a"))
		  (if (void? (perturb u2))
		      '()
		      (c:call (c:constructor-name (perturb u2))
			      (if (void? u2)
				  '()
				  (c:union (unperturb v2)
					   (c:slot v2 (c:slot v "x" "d") "p")
					   code2)))))))
	    widener-instances))
	  (generate-slot-names (unperturb v2))
	  (get-union-values (unperturb v2)))))
       ((and (or (vlad-empty-list? v1)
		 (vlad-true? v1)
		 (vlad-false? v1)
		 (primitive-procedure? v1))
	     (every-bundlable? v1 v2))
	;; In all cases, the result will be void so this case should never
	;; happen.
	'error)
       ((or (and (vlad-real? v1) (every-bundlable? v1 v2))
	    ;; here I am: need to check conformance
	    ;;            even when one or both arguments is void
	    ;;            and even when one or both arguments is deep
	    (and (perturbation-tagged-value? v1) (some-bundlable? v1 v2))
	    (and (bundle? v1) (some-bundlable? v1 v2))
	    (and (sensitivity-tagged-value? v1) (some-bundlable? v1 v2))
	    (and (reverse-tagged-value? v1) (some-bundlable? v1 v2)))
	(c:call (c:constructor-name (bundle (vlad-cons v1 v2)))
		(if (void? v1) '() (c:slot v "x" "a"))
		(if (void? v2) '() (c:slot v "x" "d"))))
       ((bundle-aggregates-match? v1 v2)
	(let ((v0 (bundle (vlad-cons v1 v2))))
	 (c:call*
	  (c:constructor-name v0)
	  (map (lambda (code3a code3b v3 v3a v3b)
		(if (void? v3)
		    '()
		    (c:call
		     (c:builtin-name "bundle" (vlad-cons  v3a v3b))
		     (if (void? (vlad-cons v3a v3b))
			 '()
			 (c:call
			  (c:constructor-name (vlad-cons v3a v3b))
			  (if (void? v3a)
			      '()
			      (c:slot v1 (c:slot v "x" "a") code3a))
			  (if (void? v3b)
			      '()
			      (c:slot v2 (c:slot v "x" "d") code3b)))))))
	       (generate-slot-names v1)
	       (generate-slot-names v2)
	       (aggregate-value-values v0)
	       (aggregate-value-values v1)
	       (aggregate-value-values v2)))))
       (else (c:panic (bundle v) "Arguments to bundle do not conform")))))
    (else (c:panic (bundle v) "Arguments to bundle do not conform"))))))

(define (generate-sensitize-definitions widener-instances)
 (generate-unary-ad-definitions
  'sensitize
  (lambda (v)
   (and (not (perturbation-tagged-value? v))
	(not (bundle? v))
	(not (sensitivity-tagged-value? v))
	(not (reverse-tagged-value? v))))
  sensitize "sensitize"
  (lambda (v)
   (cond
    ((union? v)
     (c:dispatch
      v
      "x"
      (map (lambda (code u)
	    (c:widen (sensitize u)
		     (sensitize v)
		     (c:call (c:builtin-name "sensitize" u)
			     (if (void? u) '() (c:union v "x" code)))
		     widener-instances))
	   (generate-slot-names v)
	   (get-union-values v))))
    ((or (vlad-real? v)
	 (perturbation-tagged-value? v)
	 (bundle? v)
	 (sensitivity-tagged-value? v)
	 (reverse-tagged-value? v))
     (c:call (c:constructor-name (sensitize v)) (if (void? v) '() "x")))
    ((or (nonrecursive-closure? v) (recursive-closure? v) (tagged-pair? v))
     (c:call* (c:constructor-name (sensitize v))
	      (map (lambda (code1 v1)
		    (if (void? (sensitize v1))
			'()
			(c:call (c:builtin-name "sensitize" v1)
				(if (void? v1) '() (c:slot v "x" code1)))))
		   (generate-slot-names v)
		   (aggregate-value-values v))))
    (else 'error)))))

(define (generate-unsensitize-definitions widener-instances)
 (generate-unary-ad-definitions
  'unsensitize
  (lambda (v) (and (sensitivity-value? v) (not (sensitivity-tagged-value? v))))
  unsensitize "unsensitize"
  (lambda (v)
   (cond
    ((union? v)
     (c:dispatch
      v
      "x"
      (map (lambda (code u)
	    (c:widen (unsensitize u)
		     (unsensitize v)
		     (c:call (c:builtin-name "unsensitize" u)
			     (if (void? u) '() (c:union v "x" code)))
		     widener-instances))
	   (generate-slot-names v)
	   (get-union-values v))))
    ((sensitivity-tagged-value? v) (c:slot v "x" "p"))
    ((and
      (or (nonrecursive-closure? v) (recursive-closure? v) (tagged-pair? v))
      (not (empty-abstract-value? (unsensitize v))))
     (c:call* (c:constructor-name (unsensitize v))
	      (map (lambda (code1 v1)
		    (if (void? (unsensitize v1))
			'()
			(c:call (c:builtin-name "unsensitize" v1)
				(if (void? v1) '() (c:slot v "x" code1)))))
		   (generate-slot-names v)
		   (aggregate-value-values v))))
    (else (c:panic (unsensitize v)
		   "Argument to unsensitize is not a sensitivity value"))))))

(define (plus-before v)
 (cond
  ((union? v) (get-union-values v))
  ((vlad-pair? v)
   (let ((v1 (vlad-car v)) (v2 (vlad-cdr v)))
    (cond
     ((union? v1) (map (lambda (u1) (vlad-cons u1 v2)) (get-union-values v1)))
     ((union? v2) (map (lambda (u2) (vlad-cons v1 u2)) (get-union-values v2)))
     ((or
       (and (vlad-empty-list? v1) (vlad-empty-list? v2))
       (and (vlad-true? v1) (vlad-true? v2))
       (and (vlad-false? v1) (vlad-false? v2))
       (and (primitive-procedure? v1) (primitive-procedure? v2) (eq? v1 v2)))
      '())
     ((and (vlad-real? v1) (vlad-real? v2)) '())
     ((plus-aggregates-match? v1 v2)
      (map vlad-cons (aggregate-value-values v1) (aggregate-value-values v2)))
     (else '()))))
  (else '())))

(define (generate-plus-definitions widener-instances)
 (generate-binary-ad-definitions
  'plus
  (lambda (v) #t) (lambda (v) #f)
  identity identity plus plus-aggregates-match? plus-before "plus"
  (lambda (v)
   (cond
    ((union? v)
     (c:dispatch
      v
      "x"
      (map (lambda (code u)
	    (c:widen (plus u)
		     (plus v)
		     (c:call (c:builtin-name "plus" u)
			     (if (void? u) '() (c:union v "x" code)))
		     widener-instances))
	   (generate-slot-names v)
	   (get-union-values v))))
    ((vlad-pair? v)
     (let ((v1 (vlad-car v)) (v2 (vlad-cdr v)))
      (cond
       ((union? v1)
	(c:dispatch
	 v1
	 (c:slot v "x" "a")
	 (map (lambda (code1 u1)
	       (c:widen
		(plus (vlad-cons u1 v2))
		(plus (vlad-cons v1 v2))
		(c:call (c:builtin-name "plus" (vlad-cons u1 v2))
			(if (void? (vlad-cons u1 v2))
			    '()
			    (c:call (c:constructor-name (vlad-cons u1 v2))
				    (if (void? u1)
					'()
					(c:union v1 (c:slot v "x" "a") code1))
				    (if (void? v2) '() (c:slot v "x" "d")))))
		widener-instances))
	      (generate-slot-names v1)
	      (get-union-values v1))))
       ((union? v2)
	(c:dispatch
	 v2
	 (c:slot v "x" "d")
	 (map
	  (lambda (code2 u2)
	   (c:widen
	    (plus (vlad-cons v1 u2))
	    (plus (vlad-cons v1 v2))
	    (c:call (c:builtin-name "plus" (vlad-cons v1 u2))
		    (if (void? (vlad-cons v1 u2))
			'()
			(c:call (c:constructor-name (vlad-cons v1 u2))
				(if (void? v1) '() (c:slot v "x" "a"))
				(if (void? u2)
				    '()
				    (c:union v2 (c:slot v "x" "d") code2)))))
	    widener-instances))
	  (generate-slot-names v2)
	  (get-union-values v2))))
       ((or
	 (and (vlad-empty-list? v1) (vlad-empty-list? v2))
	 (and (vlad-true? v1) (vlad-true? v2))
	 (and (vlad-false? v1) (vlad-false? v2))
	 (and (primitive-procedure? v1) (primitive-procedure? v2) (eq? v1 v2)))
	;; In all cases, the result will be void so this case should never
	;; happen.
	'error)
       ((and (vlad-real? v1) (vlad-real? v2))
  	(c:binary
	 ;; This assumes that Scheme inexact numbers are printed as C doubles.
	 (if (void? v1) (exact->inexact v1) (c:slot v "x" "a"))
	 "+"
	 ;; This assumes that Scheme inexact numbers are printed as C doubles.
	 (if (void? v2) (exact->inexact v2) (c:slot v "x" "d"))))
       ((plus-aggregates-match? v1 v2)
	(let ((v0 (plus (vlad-cons v1 v2))))
	 (c:call*
	  (c:constructor-name v0)
	  (map (lambda (code3a code3b v3 v3a v3b)
		(if (void? v3)
		    '()
		    (c:call
		     (c:builtin-name "plus" (vlad-cons v3a v3b))
		     (if (void? (vlad-cons v3a v3b))
			 '()
			 (c:call
			  (c:constructor-name (vlad-cons v3a v3b))
			  (if (void? v3a)
			      '()
			      (c:slot v1 (c:slot v "x" "a") code3a))
			  (if (void? v3b)
			      '()
			      (c:slot v2 (c:slot v "x" "d") code3b)))))))
	       (generate-slot-names v1)
	       (generate-slot-names v2)
	       (aggregate-value-values v0)
	       (aggregate-value-values v1)
	       (aggregate-value-values v2)))))
       (else (c:panic (plus v) "Arguments to plus do not conform")))))
    (else (c:panic (plus v) "Arguments to plus do not conform"))))))

(define (generate-*j-definitions widener-instances)
 (generate-unary-ad-definitions
  '*j
  (lambda (v)
   (and (not (perturbation-tagged-value? v))
	(not (bundle? v))
	(not (sensitivity-tagged-value? v))
	(not (reverse-tagged-value? v))))
  *j "starj"
  (lambda (v)
   (cond
    ((union? v)
     (c:dispatch
      v
      "x"
      (map (lambda (code u)
	    (c:widen (*j u)
		     (*j v)
		     (c:call (c:builtin-name "starj" u)
			     (if (void? u) '() (c:union v "x" code)))
		     widener-instances))
	   (generate-slot-names v)
	   (get-union-values v))))
    ((or (vlad-real? v)
	 (perturbation-tagged-value? v)
	 (bundle? v)
	 (sensitivity-tagged-value? v)
	 (reverse-tagged-value? v))
     (c:call (c:constructor-name (*j v)) (if (void? v) '() "x")))
    ((or (nonrecursive-closure? v) (recursive-closure? v) (tagged-pair? v))
     (c:call* (c:constructor-name (*j v))
	      (map (lambda (code1 v1)
		    (if (void? (*j v1))
			'()
			(c:call (c:builtin-name "starj" v1)
				(if (void? v1) '() (c:slot v "x" code1)))))
		   (generate-slot-names v)
		   (aggregate-value-values v))))
    (else 'error)))))

(define (generate-*j-inverse-definitions widener-instances)
 (generate-unary-ad-definitions
  '*j-inverse
  (lambda (v) (and (reverse-value? v) (not (reverse-tagged-value? v))))
  *j-inverse "starj_inverse"
  (lambda (v)
   (cond
    ((union? v)
     (c:dispatch
      v
      "x"
      (map (lambda (code u)
	    (c:widen (*j-inverse u)
		     (*j-inverse v)
		     (c:call (c:builtin-name "starj_inverse" u)
			     (if (void? u) '() (c:union v "x" code)))
		     widener-instances))
	   (generate-slot-names v)
	   (get-union-values v))))
    ((reverse-tagged-value? v) (c:slot v "x" "p"))
    ((and
      (or (nonrecursive-closure? v) (recursive-closure? v) (tagged-pair? v))
      (not (empty-abstract-value? (*j-inverse v))))
     (c:call* (c:constructor-name (*j-inverse v))
	      (map (lambda (code1 v1)
		    (if (void? (*j-inverse v1))
			'()
			(c:call (c:builtin-name "starj_inverse" v1)
				(if (void? v1) '() (c:slot v "x" code1)))))
		   (generate-slot-names v)
		   (aggregate-value-values v))))
    (else (c:panic (*j-inverse v)
		   "Argument to *j-inverse is not a reverse value"))))))

(define (generate-if
	 v0 v1 v2 v3 code1 code2 code3 function-instances widener-instances)
 (cond
  ((and (some vlad-false? (union-members v1))
	(some (lambda (u1) (not (vlad-false? u1))) (union-members v1)))
   (c:conditional
    (c:binary
     ;; The type tag is always unboxed.
     (c:tag v1 code1)
     "!="
     ;; This uses per-union tags here instead of per-program tags.
     (position-if vlad-false? (union-members v1)))
    (generate-call
     v0 v2 (vlad-empty-list) code2 '() function-instances widener-instances)
    (generate-call
     v0 v3 (vlad-empty-list) code3 '() function-instances widener-instances)))
  ((some vlad-false? (union-members v1))
   (generate-call
    v0 v3 (vlad-empty-list) code3 '() function-instances widener-instances))
  ((some (lambda (u1) (not (vlad-false? u1))) (union-members v1))
   (generate-call
    v0 v2 (vlad-empty-list) code2 '() function-instances widener-instances))
  (else (internal-error))))

(define (generate-if-and-function-definition
	 instance bs function-instances widener-instances p?)
 (cond
  ((if-instance? instance)
   (let* ((v (if-instance-v instance)) (v0 (if-return-value v)))
    (c:specifier-function-definition
     #t #t #f v0
     (c:function-declarator (c:builtin-name "if_procedure" v)
			    (c:specifier-parameter v "x"))
     (c:return
      (cond
       ((union? v)
	(c:dispatch
	 v
	 "x"
	 (map
	  (lambda (code u)
	   (if (vlad-pair? u)
	       (let ((v1 (vlad-car u)) (v23 (vlad-cdr u)))
		(cond
		 ((union? v23)
		  (c:dispatch
		   v23
		   (if (void? v23) '() (c:slot u (c:union v "x" code) "d"))
		   (map
		    (lambda (code23 u23)
		     (if (vlad-pair? u23)
			 (let ((v2 (vlad-car u23)) (v3 (vlad-cdr u23)))
			  (generate-if
			   v0
			   v1
			   v2
			   v3
			   (c:slot u (c:union v "x" code) "a")
			   (if (void? v2)
			       '()
			       (c:slot
				u23
				(c:union v23
					 (c:slot u (c:union v "x" code) "d")
					 code23)
				"a"))
			   (if (void? v3)
			       '()
			       (c:slot
				u23
				(c:union v23
					 (c:slot u (c:union v "x" code) "d")
					 code23)
				"d"))
			   function-instances
			   widener-instances))
			 (c:panic v0 "Argument to if-procedure is invalid")))
		    (generate-slot-names v23)
		    (get-union-values v23))))
		 ((vlad-pair? v23)
		  (let ((v2 (vlad-car v23)) (v3 (vlad-cdr v23)))
		   (generate-if
		    v0
		    v1
		    v2
		    v3
		    (c:slot u (c:union v "x" code) "a")
		    (if (void? v2)
			'()
			(c:slot v23 (c:slot u (c:union v "x" code) "d") "a"))
		    (if (void? v3)
			'()
			(c:slot v23 (c:slot u (c:union v "x" code) "d") "d"))
		    function-instances
		    widener-instances)))
		 (else (c:panic v0 "Argument to if-procedure is invalid"))))
	       (c:panic v0 "Argument to if-procedure is invalid")))
	  (generate-slot-names v)
	  (get-union-values v))))
       ((vlad-pair? v)
	(let ((v1 (vlad-car v)) (v23 (vlad-cdr v)))
	 (cond
	  ((union? v23)
	   (c:dispatch
	    v23
	    (if (void? v23) '() (c:slot v "x" "d"))
	    (map (lambda (code23 u23)
		  (if (vlad-pair? u23)
		      (let ((v2 (vlad-car u23)) (v3 (vlad-cdr u23)))
		       (generate-if
			v0
			v1
			v2
			v3
			(c:slot v "x" "a")
			(if (void? v2)
			    '()
			    (c:slot
			     u23 (c:union v23 (c:slot v "x" "d") code23) "a"))
			(if (void? v3)
			    '()
			    (c:slot
			     u23 (c:union v23 (c:slot v "x" "d") code23) "d"))
			function-instances
			widener-instances))
		      (c:panic v0 "Argument to if-procedure is invalid")))
		 (generate-slot-names v23)
		 (get-union-values v23))))
	  ((vlad-pair? v23)
	   (let ((v2 (vlad-car v23)) (v3 (vlad-cdr v23)))
	    (generate-if
	     v0
	     v1
	     v2
	     v3
	     (c:slot v "x" "a")
	     (if (void? v2) '() (c:slot v23 (c:slot v "x" "d") "a"))
	     (if (void? v3) '() (c:slot v23 (c:slot v "x" "d") "d"))
	     function-instances
	     widener-instances)))
	  (else (c:panic v0 "Argument to if-procedure is invalid")))))
       (else (c:panic v0 "Argument to if-procedure is invalid")))))))
  ((function-instance? instance)
   (let ((v1 (function-instance-v1 instance))
	 (v2 (function-instance-v2 instance)))
    (c:specifier-function-definition
     #t p? #f
     (abstract-apply v1 v2)
     (c:function-declarator (c:function-name v1 v2 function-instances)
			    (c:specifier-parameter v1 "c")
			    (c:specifier-parameter v2 "x"))
     (c:return
      (generate-destructure
       (abstract-apply v1 v2)
       (closure-parameter v1)
       v2
       "x"
       (lambda (alist codes)
	(let ((vs (map widen-abstract-value (construct-environment v1 alist))))
	 (c:statement-expression
	  codes
	  (generate-letrec-bindings
	   (closure-body v1)
	   vs
	   (closure-variables v1)
	   (cond ((nonrecursive-closure? v1) '())
		 ((recursive-closure? v1)
		  (vector->list (recursive-closure-procedure-variables v1)))
		 (else (internal-error)))
	   widener-instances)
	  (c:widen
	   (abstract-eval1 (closure-body v1) vs)
	   (abstract-apply v1 v2)
	   (generate-expression
	    (closure-body v1)
	    vs
	    v1
	    (closure-variables v1)
	    (cond ((nonrecursive-closure? v1) '())
		  ((recursive-closure? v1)
		   (vector->list (recursive-closure-procedure-variables v1)))
		  (else (internal-error)))
	    bs
	    function-instances
	    widener-instances)
	   widener-instances)))))))))
  (else (internal-error))))

(define (generate-if-and-function-definitions
	 bs function-instances widener-instances instances1-instances2)
 ;; abstraction
 (append
  ;; abstraction
  (map (lambda (instance)
	(generate-if-and-function-definition
	 instance bs function-instances widener-instances #t))
       (first instances1-instances2))
  ;; abstraction
  (map (lambda (instance)
	(generate-if-and-function-definition
	 instance bs function-instances widener-instances #f))
       (second instances1-instances2))))

;;; Top-level generator

(define (generate e bs)
 (with-abstract
  (lambda ()
   (determine-void?!)
   (for-each-indexed (lambda (x i) (set-variable-index! x i)) *variables*)
   (let* ((function-instances (time-it (all-function-instances)))
	  (widener-instances (time-it (all-widener-instances function-instances)))
	  (vs1-vs2 (time-it (all-nested-abstract-values widener-instances)))
	  (instances1-instances2 (time-it (all-instances1-instances2 function-instances))))
    (determine-void?!)
    (set! *frozen?* #t)
    (for-each-indexed
     (lambda (v i)
      (cond ((nonrecursive-closure? v) (set-nonrecursive-closure-c:index! v i))
	    ((recursive-closure? v) (set-recursive-closure-c:index! v i))
	    ((perturbation-tagged-value? v)
	     (set-perturbation-tagged-value-c:index! v i))
	    ((bundle? v) (set-bundle-c:index! v i))
	    ((sensitivity-tagged-value? v)
	     (set-sensitivity-tagged-value-c:index! v i))
	    ((reverse-tagged-value? v) (set-reverse-tagged-value-c:index! v i))
	    ((tagged-pair? v) (set-tagged-pair-c:index! v i))
	    ((union? v) (set-union-c:index! v i))
	    (else (set! *c:indices* (cons (cons v i) *c:indices*)))))
     (append (first vs1-vs2) (second vs1-vs2)))
    (for-each
     (lambda (v)
      (cond ((nonrecursive-closure? v) (set-nonrecursive-closure-boxed?! v #t))
	    ((recursive-closure? v) (set-recursive-closure-boxed?! v #t))
	    ((perturbation-tagged-value? v)
	     (set-perturbation-tagged-value-boxed?! v #t))
	    ((bundle? v) (set-bundle-boxed?! v #t))
	    ((sensitivity-tagged-value? v)
	     (set-sensitivity-tagged-value-boxed?! v #t))
	    ((reverse-tagged-value? v) (set-reverse-tagged-value-boxed?! v #t))
	    ((tagged-pair? v) (set-tagged-pair-boxed?! v #t))
	    ((union? v) (set-union-boxed?! v #t))
	    (else (internal-error))))
     (second vs1-vs2))
    ;; abstraction
    (list
     "#include <math.h>" #\newline
     "#include <stdio.h>" #\newline
     "#include <stdlib.h>" #\newline
     "#include <gc/gc.h>" #\newline
     "#define INLINE inline __attribute__ ((always_inline))" #\newline
     "#define NORETURN __attribute__ ((noreturn))" #\newline
     (time-it (generate-struct-and-union-declarations vs1-vs2))
     (time-it (generate-constructor-declarations vs1-vs2))
     (time-it (generate-widener-declarations widener-instances))
     (time-it (generate-panic-declarations vs1-vs2))
     (time-it (generate-real*real-primitive-declarations '+ (abstract-real) "add"))
     (time-it (generate-real*real-primitive-declarations '- (abstract-real) "minus"))
     (time-it (generate-real*real-primitive-declarations '* (abstract-real) "times"))
     (time-it (generate-real*real-primitive-declarations '/ (abstract-real) "divide"))
     (time-it (generate-real-primitive-declarations 'sqrt (abstract-real) "sqrt"))
     (time-it (generate-real-primitive-declarations 'exp (abstract-real) "exp"))
     (time-it (generate-real-primitive-declarations 'log (abstract-real) "log"))
     (time-it (generate-real-primitive-declarations 'sin (abstract-real) "sin"))
     (time-it (generate-real-primitive-declarations 'cos (abstract-real) "cos"))
     (time-it (generate-real*real-primitive-declarations 'atan (abstract-real) "atantwo"))
     (time-it (generate-real*real-primitive-declarations '= (abstract-boolean) "eq"))
     (time-it (generate-real*real-primitive-declarations '< (abstract-boolean) "lt"))
     (time-it (generate-real*real-primitive-declarations '> (abstract-boolean) "gt"))
     (time-it (generate-real*real-primitive-declarations '<= (abstract-boolean) "le"))
     (time-it (generate-real*real-primitive-declarations '>= (abstract-boolean) "ge"))
     (time-it (generate-real-primitive-declarations 'zero? (abstract-boolean) "iszero"))
     (time-it (generate-real-primitive-declarations 'positive? (abstract-boolean) "positive"))
     (time-it (generate-real-primitive-declarations 'negative? (abstract-boolean) "negative"))
     (time-it (generate-type-predicate-declarations 'null? "null"))
     (time-it (generate-type-predicate-declarations 'boolean? "boolean"))
     (time-it (generate-type-predicate-declarations 'real? "is_real"))
     (time-it (generate-type-predicate-declarations 'pair? "pair"))
     (time-it (generate-type-predicate-declarations 'procedure? "procedure"))
     ;; The perturbation?, forward?, sensitivity? and reverse? primitives are not
     ;; referentially transparent and violate the forward-transformation rule for
     ;; functions that only rearrange data.
     (time-it (generate-type-predicate-declarations 'perturbation? "perturbation"))
     (time-it (generate-type-predicate-declarations 'forward? "forward"))
     (time-it (generate-type-predicate-declarations 'sensitivty? "sensitivity"))
     (time-it (generate-type-predicate-declarations 'reverse? "reverse"))
     (time-it (c:specifier-function-declaration
	       #t #t #f (abstract-real) (c:function-declarator "read_real")))
     (time-it (generate-real-primitive-declarations 'real (abstract-real) "real"))
     (time-it (c:specifier-function-declaration
	       #t #t #f (abstract-real)
	       (c:function-declarator
		"write_real" (c:specifier-parameter (abstract-real) "x"))))
     (time-it (generate-real-primitive-declarations 'write-real (abstract-real) "write_real"))
     (time-it (generate-unary-ad-declarations 'zero (lambda (v) #t) zero "zero"))
     (time-it (generate-unary-ad-declarations
	       'perturb
	       (lambda (v)
		(and (not (perturbation-tagged-value? v))
		     (not (bundle? v))
		     (not (sensitivity-tagged-value? v))
		     (not (reverse-tagged-value? v))))
	       perturb "perturb"))
     (time-it (generate-unary-ad-declarations
	       'unperturb
	       (lambda (v)
		(and (perturbation-value? v) (not (perturbation-tagged-value? v))))
	       unperturb "unperturb"))
     (time-it (generate-unary-ad-declarations
	       'primal (lambda (v) (and (forward-value? v) (not (bundle? v))))
	       primal "primal"))
     (time-it (generate-unary-ad-declarations
	       'tangent (lambda (v) (and (forward-value? v) (not (bundle? v))))
	       tangent "tangent"))
     (time-it (generate-binary-ad-declarations
	       'bundle
	       (lambda (v)
		(and (not (perturbation-tagged-value? v))
		     (not (bundle? v))
		     (not (sensitivity-tagged-value? v))
		     (not (reverse-tagged-value? v))))
	       perturbation-tagged-value? perturb unperturb bundle
	       bundle-aggregates-match? bundle-before "bundle"))
     (time-it (generate-unary-ad-declarations
	       'sensitize
	       (lambda (v)
		(and (not (perturbation-tagged-value? v))
		     (not (bundle? v))
		     (not (sensitivity-tagged-value? v))
		     (not (reverse-tagged-value? v))))
	       sensitize "sensitize"))
     (time-it (generate-unary-ad-declarations
	       'unsensitize
	       (lambda (v)
		(and (sensitivity-value? v) (not (sensitivity-tagged-value? v))))
	       unsensitize "unsensitize"))
     (time-it (generate-binary-ad-declarations
	       'plus (lambda (v) #t) (lambda (v) #f) identity identity plus
	       plus-aggregates-match? plus-before "plus"))
     (time-it (generate-unary-ad-declarations
	       '*j
	       (lambda (v)
		(and (not (perturbation-tagged-value? v))
		     (not (bundle? v))
		     (not (sensitivity-tagged-value? v))
		     (not (reverse-tagged-value? v))))
	       *j "starj"))
     (time-it (generate-unary-ad-declarations
	       '*j-inverse
	       (lambda (v) (and (reverse-value? v) (not (reverse-tagged-value? v))))
	       *j-inverse "starj_inverse"))
     (time-it (generate-if-and-function-declarations
	       function-instances instances1-instances2))
     (time-it (c:function-declaration #f #f #f "int" (c:function-declarator "main")))
     (time-it (generate-constructor-definitions vs1-vs2))
     (time-it (generate-widener-definitions widener-instances))
     (time-it (generate-panic-definitions vs1-vs2))
     (time-it (generate-real*real-primitive-definitions
	       '+ (abstract-real) "add" "+"
	       (lambda (code1 code2) (c:binary code1 "+" code2))))
     (time-it (generate-real*real-primitive-definitions
	       '- (abstract-real) "minus" "-"
	       (lambda (code1 code2) (c:binary code1 "-" code2))))
     (time-it (generate-real*real-primitive-definitions
	       '* (abstract-real) "times" "*"
	       (lambda (code1 code2) (c:binary code1 "*" code2))))
     (time-it (generate-real*real-primitive-definitions
	       '/ (abstract-real) "divide" "/"
	       (lambda (code1 code2) (c:binary code1 "/" code2))))
     (time-it (generate-real-primitive-definitions
	       'sqrt (abstract-real) "sqrt" "sqrt"
	       (lambda (code) (c:call "sqrt" code))))
     (time-it (generate-real-primitive-definitions
	       'exp (abstract-real) "exp" "exp"
	       (lambda (code) (c:call "exp" code))))
     (time-it (generate-real-primitive-definitions
	       'log (abstract-real) "log" "log"
	       (lambda (code) (c:call "log" code))))
     (time-it (generate-real-primitive-definitions
	       'sin (abstract-real) "sin" "sin"
	       (lambda (code) (c:call "sin" code))))
     (time-it (generate-real-primitive-definitions
	       'cos (abstract-real) "cos" "cos"
	       (lambda (code) (c:call "cos" code))))
     (time-it (generate-real*real-primitive-definitions
	       'atan (abstract-real) "atantwo" "atan"
	       (lambda (code1 code2) (c:call "atan2" code1 code2))))
     (time-it (generate-real*real-primitive-definitions
	       '= (abstract-boolean) "eq" "=" (c:binary-boolean "==")))
     (time-it (generate-real*real-primitive-definitions
	       '< (abstract-boolean) "lt" "<" (c:binary-boolean "<")))
     (time-it (generate-real*real-primitive-definitions
	       '> (abstract-boolean) "gt" ">" (c:binary-boolean ">")))
     (time-it (generate-real*real-primitive-definitions
	       '<= (abstract-boolean) "le" "<=" (c:binary-boolean "<=")))
     (time-it (generate-real*real-primitive-definitions
	       '>= (abstract-boolean) "ge" ">=" (c:binary-boolean ">=")))
     (time-it (generate-real-primitive-definitions
	       'zero? (abstract-boolean) "iszero" "zero?" (c:unary-boolean "==")))
     (time-it (generate-real-primitive-definitions
	       'positive? (abstract-boolean) "positive" "positive?" (c:unary-boolean ">")))
     (time-it (generate-real-primitive-definitions
	       'negative? (abstract-boolean) "negative" "negative?" (c:unary-boolean "<")))
     (time-it (generate-type-predicate-definitions 'null? "null" vlad-empty-list?))
     (time-it (generate-type-predicate-definitions 'boolean? "boolean" vlad-boolean?))
     (time-it (generate-type-predicate-definitions 'real? "is_real" vlad-real?))
     (time-it (generate-type-predicate-definitions 'pair? "pair" vlad-pair?))
     ;; needs work: This should probably return #f for any transformed procedure.
     (time-it (generate-type-predicate-definitions 'procedure? "procedure" vlad-procedure?))
     ;; The perturbation?, forward?, sensitivity? and reverse? primitives are not
     ;; referentially transparent and violate the forward-transformation rule for
     ;; functions that only rearrange data.
     (time-it (generate-type-predicate-definitions 'perturbation? "perturbation" perturbation-value?))
     (time-it (generate-type-predicate-definitions 'forward? "forward" forward-value?))
     (time-it (generate-type-predicate-definitions 'sensitivty? "sensitivity" sensitivity-value?))
     (time-it (generate-type-predicate-definitions 'reverse? "reverse" reverse-value?))
     (time-it (c:specifier-function-definition
	       #t #t #f (abstract-real) (c:function-declarator "read_real")
	       ;; abstraction
	       (list (c:specifier-declaration (abstract-real) "x")
		     ;; abstraction
		     "scanf(\"%lf\",&x);"
		     (c:return "x"))))
     (time-it (generate-real-primitive-definitions
	       'real (abstract-real) "real" "real" (lambda (code) code)))
     (time-it (c:specifier-function-definition
	       #t #t #f (abstract-real)
	       (c:function-declarator
		"write_real" (c:specifier-parameter (abstract-real) "x"))
	       ;; abstraction
	       (list
		;; abstraction
		"printf(\"%.18lg\\n\",x);"
		(c:return "x"))))
     (time-it (generate-real-primitive-definitions
	       'write-real (abstract-real) "write_real" "write-real"
	       (lambda (code) (c:call "write_real" code))))
     (time-it (generate-zero-definitions widener-instances))
     (time-it (generate-perturb-definitions widener-instances))
     (time-it (generate-unperturb-definitions widener-instances))
     (time-it (generate-primal-definitions widener-instances))
     (time-it (generate-tangent-definitions widener-instances))
     (time-it (generate-bundle-definitions widener-instances))
     (time-it (generate-sensitize-definitions widener-instances))
     (time-it (generate-unsensitize-definitions widener-instances))
     (time-it (generate-plus-definitions widener-instances))
     (time-it (generate-*j-definitions widener-instances))
     (time-it (generate-*j-inverse-definitions widener-instances))
     (time-it (generate-if-and-function-definitions
	       bs function-instances widener-instances instances1-instances2))
     (let ((vs (environment-binding-values
		(first (expression-environment-bindings e)))))
      (time-it (c:function-definition
		#f #f #f
		"int"
		(c:function-declarator "main")
		;; abstraction
		(list
		 (generate-letrec-bindings
		  e vs (free-variables e) '() widener-instances)
		 (if (void? (abstract-eval1
			     e
			     (environment-binding-values
			      (first (expression-environment-bindings e)))))
		     '()
		     ;; abstraction
		     (list
		      (generate-expression e
					   vs
					   ;; A placeholder.
					   (empty-abstract-value)
					   (free-variables e)
					   '()
					   bs
					   function-instances
					   widener-instances)
		      ;; abstraction
		      ";"))
		 (c:return "0"))))))))))

(define (generate-file code pathname)
 (call-with-output-file (replace-extension pathname "c")
  (lambda (output-port)
   (let loop ((code code))
    (cond ((char? code) (write-char code output-port))
	  ((string? code) (display code output-port))
	  ((number? code) (write code output-port))
	  ((pair? code) (loop (car code)) (loop (cdr code)))
	  ((null? code) #f)
	  (else (internal-error)))))))

;;; New Code Generator

(define (unit->code u)
 (cond
  ;; This assumes that Scheme inexact numbers are printed as C doubles.
  ((real? u) (exact->inexact u))
  ((name-unit? u) (name-unit-code u))
  ((call-unit? u)
   (c:call* (call-unit-code u)
	    (map-reduce append
			'()
			(lambda (x) (map unit->code (flatten x)))
			(call-unit-xs u))))
  ((panic-unit? u) (c:call "panic" (list "\"" (panic-unit-x u) "\"")))
  ((+-unit? u)
   (assert (not (and (real? (+-unit-x u)) (real? (+-unit-y u)))))
   (c:parenthesize
    (c:binary (unit->code (+-unit-x u)) "+" (unit->code (+-unit-y u)))))
  ((--unit? u)
   (assert (not (and (real? (--unit-x u)) (real? (--unit-y u)))))
   (c:parenthesize
    (c:binary (unit->code (--unit-x u)) "-" (unit->code (--unit-y u)))))
  ((*-unit? u)
   (assert (not (and (real? (*-unit-x u)) (real? (*-unit-y u)))))
   (c:parenthesize
    (c:binary (unit->code (*-unit-x u)) "*" (unit->code (*-unit-y u)))))
  ((/-unit? u)
   (assert (not (and (real? (/-unit-x u)) (real? (/-unit-y u)))))
   (c:parenthesize
    (c:binary (unit->code (/-unit-x u)) "/" (unit->code (/-unit-y u)))))
  ((sqrt-unit? u)
   (assert (not (real? (sqrt-unit-x u))))
   (c:call "sqrt" (unit->code (sqrt-unit-x u))))
  ((exp-unit? u)
   (assert (not (real? (exp-unit-x u))))
   (c:call "exp" (unit->code (exp-unit-x u))))
  ((log-unit? u)
   (assert (not (real? (log-unit-x u))))
   (c:call "log" (unit->code (log-unit-x u))))
  ((sin-unit? u)
   (assert (not (real? (sin-unit-x u))))
   (c:call "sin" (unit->code (sin-unit-x u))))
  ((cos-unit? u)
   (assert (not (real? (cos-unit-x u))))
   (c:call "cos" (unit->code (cos-unit-x u))))
  ((atan-unit? u)
   (assert (not (and (real? (atan-unit-x u)) (real? (atan-unit-y u)))))
   (c:call "atan2" (unit->code (atan-unit-x u)) (unit->code (atan-unit-y u))))
  ((=-unit? u)
   (assert (not (and (real? (=-unit-x u)) (real? (=-unit-y u)))))
   (c:parenthesize
    ((c:binary-boolean "=")
     (unit->code (=-unit-x u)) (unit->code (=-unit-y u)))))
  ((<-unit? u)
   (assert (not (and (real? (<-unit-x u)) (real? (<-unit-y u)))))
   (c:parenthesize
    ((c:binary-boolean "<")
     (unit->code (<-unit-x u)) (unit->code (<-unit-y u)))))
  ((>-unit? u)
   (assert (not (and (real? (>-unit-x u)) (real? (>-unit-y u)))))
   (c:parenthesize
    ((c:binary-boolean ">")
     (unit->code (>-unit-x u)) (unit->code (>-unit-y u)))))
  ((<=-unit? u)
   (assert (not (and (real? (<=-unit-x u)) (real? (<=-unit-y u)))))
   (c:parenthesize
    ((c:binary-boolean "<=")
     (unit->code (<=-unit-x u)) (unit->code (<=-unit-y u)))))
  ((>=-unit? u)
   (assert (not (and (real? (>=-unit-x u)) (real? (>=-unit-y u)))))
   (c:parenthesize
    ((c:binary-boolean ">=")
     (unit->code (>=-unit-x u)) (unit->code (>=-unit-y u)))))
  ((zero?-unit? u)
   (assert (not (real? (zero?-unit-x u))))
   (c:parenthesize ((c:unary-boolean "=") (unit->code (zero?-unit-x u)))))
  ((positive?-unit? u)
   (assert (not (real? (positive?-unit-x u))))
   (c:parenthesize ((c:unary-boolean ">") (unit->code (positive?-unit-x u)))))
  ((negative?-unit? u)
   (assert (not (real? (negative?-unit-x u))))
   (c:parenthesize ((c:unary-boolean "<") (unit->code (negative?-unit-x u)))))
  ((read-real-unit? u)
   (assert (vlad-empty-list? (read-real-unit-x u)))
   (c:call "read_real"))
  ((write-real-unit? u)
   (assert (not (real? (sqrt-unit-x u))))
   (c:call "write_real" (unit->code (write-real-unit-x u))))
  (else (internal-error))))

(define (new-generate-struct-and-union-declarations vs1-vs2)
 ;; generate -~-> c:
 ;; abstraction
 (list
  ;; This generates forward declarations for the struct and union tags.
  ;; abstraction
  (map (lambda (v)
	;; abstraction
	(list
	 ;; abstraction
	 (list (c:specifier v) ";" #\newline)))
       (second vs1-vs2))
  ;; abstraction
  (map (lambda (v)
	(if (or (and (not (union? v)) (scalar-value? v)) (void? v))
	    '()
	    ;; abstraction
	    (list
	     ;; abstraction
	     (list
	      (c:specifier v)
	      "{"
	      ;; needs work: to sort slots
	      (map (lambda (u)
		    (c:specifier-declaration
		     (name-unit-abstract-value u) (name-unit-code u)))
		   (flatten (with-symbolic (lambda () (unitify v '() #f #f)))))
	      "}"
	      ";"
	      #\newline))))
       (first vs1-vs2))
  ;; abstraction
  (map
   (lambda (v)
    ;; abstraction
    (list
     ;; abstraction
     (list (c:specifier v)
	   "{"
	   ;; needs work: to sort slots
	   (map (lambda (u)
		 (c:specifier-declaration
		  (name-unit-abstract-value u) (name-unit-code u)))
		(flatten (with-symbolic (lambda () (unitify v '() #t #f)))))
	   "}"
	   ";"
	   #\newline)))
   (second vs1-vs2))))

(define (new-generate-unary-ad-declarations s descend? f code)
 ;; here I am: The result of f might violate the syntactic constraints.
 ;; abstraction
 (map (lambda (v)
       (let ((v0 (f v)))
	(c:specifier-function-declaration
	 ;; The call to f might issue "might" warnings and might return an
	 ;; empty abstract value.
	 #t #f #f v0
	 (c:function-declarator*
	  (c:builtin-name code v)
	  (map (lambda (u)
		(c:specifier-parameter
		 (name-unit-abstract-value u) (name-unit-code u)))
	       (flatten (with-symbolic (lambda () (unitify v "x" #f #f)))))))))
      (second (all-sorted-unary-ad s descend?))))

(define (new-generate-binary-ad-declarations
	 s descend? f? f f-inverse g aggregates-match? before code)
 ;; here I am: The results of f, f-inverse, and g might violate the syntactic
 ;;            constraints.
 ;; abstraction
 (map (lambda (v)
       (let ((v0 (g v)))
	(c:specifier-function-declaration
	 ;; The call to g might issue "might" warnings and might return an
	 ;; empty abstract value.
	 #t #f #f v0
	 (c:function-declarator*
	  (c:builtin-name code v)
	  (map (lambda (u)
		(c:specifier-parameter
		 (name-unit-abstract-value u) (name-unit-code u)))
	       (flatten (with-symbolic (lambda () (unitify v "x" #f #f)))))))))
      (second (all-sorted-binary-ad
	       s descend? f? f f-inverse aggregates-match? before))))

(define (new-generate-function-declarations
	 function-instances instances1-instances2)
 ;; abstraction
 (map (lambda (instance)
       (assert (function-instance? instance))
       (let* ((v1 (function-instance-v1 instance))
	      (v2 (function-instance-v2 instance))
	      (v0 (abstract-apply v1 v2)))
	(c:specifier-function-declaration
	 #t #f #f v0
	 (c:function-declarator*
	  (c:function-name v1 v2 function-instances)
	  (append
	   (map (lambda (u)
		 (c:specifier-parameter
		  (name-unit-abstract-value u) (name-unit-code u)))
		(flatten (with-symbolic (lambda () (unitify v1 "x1" #f #f)))))
	   (map (lambda (u)
		 (c:specifier-parameter
		  (name-unit-abstract-value u) (name-unit-code u)))
		(flatten
		 (with-symbolic (lambda () (unitify v2 "x2" #f #f))))))))))
      (second instances1-instances2)))

(define (new-generate-body v0 v)
 ;; abstraction
 (list
  (c:specifier-declaration (abstractify v0) "r")
  (let loop ((v0 v0) (v v))
   (let ((v0-abstract (abstractify v0)) (v-abstract (abstractify v)))
    (unless (and (not (abstract-real? v0))
		 (not (abstract-real? v))
		 (abstract-value-subset? v-abstract v0-abstract))
     (format #t "debugging1~%"))
    (assert (and (not (abstract-real? v0))
		 (not (abstract-real? v))
		 (abstract-value-subset? v-abstract v0-abstract)))
    (cond
     ;; abstraction
     ((empty-abstract-value? v-abstract) (list (unit->code v) ";"))
     ((union? v)
      (c:new-dispatch (unit->code (union-tag v))
		      (map (lambda (v) (loop v0 v)) (get-union-values v))))
     ((unit? v0)
      (cond
       ((and (unit? v) (abstract-value=? v-abstract v0-abstract))
	(c:assignment (unit->code v0) (unit->code v)))
       ((abstract-real? (unit-abstract-value v0))
	(unless (vlad-real? v-abstract) (format #t "debugging2~%"))
	(assert (vlad-real? v-abstract))
	(c:assignment (unit->code v0) (unit->code v)))
       (else
	;; abstraction
	(list (c:assignment
	       (unit->code v0)
	       (c:pointer-cast
		(c:specifier v0-abstract)
		;; We don't check for out of memory.
		(c:call "GC_malloc" (c:sizeof (c:specifier v0-abstract)))))
	      (loop (unroll v0) v)))))
     ((union? v0)
      (if (union? v-abstract)
	  (loop v0 (unroll v))
	  (let ((i (positionp abstract-value=?
			      v-abstract
			      (get-union-values v0-abstract))))
	   (unless i
	    (format #t "debugging3~%")
	    (write (debugging-externalize v0))
	    (newline)
	    (write (debugging-externalize v))
	    (newline)
	    (write (debugging-externalize v0-abstract))
	    (newline)
	    (write (debugging-externalize v-abstract))
	    (newline))
	   (assert i)
	   ;; abstraction
	   (list (c:assignment (unit->code (union-tag v0)) i)
		 (loop (list-ref (get-union-values v0) i) v)))))
     ((scalar-value? v0)
      (unless (abstract-value=? v-abstract v0-abstract)
       (format #t "debugging4~%"))
      (assert (abstract-value=? v-abstract v0-abstract))
      ;; abstraction
      (if (unit? v) (list (unit->code v) ";") '()))
     ((unit? v)
      (unless (not (abstract-real? (unit-abstract-value v)))
       (format #t "debugging5~%"))
      (assert (not (abstract-real? (unit-abstract-value v))))
      (loop v0 (unroll v)))
     ((or (and (nonrecursive-closure? v0)
	       (nonrecursive-closure? v)
	       (dereferenced-nonrecursive-closure-match? v0 v))
	  (and (recursive-closure? v0)
	       (recursive-closure? v)
	       (dereferenced-recursive-closure-match? v0 v))
	  (and (perturbation-tagged-value? v0) (perturbation-tagged-value? v))
	  (and (bundle? v0) (bundle? v))
	  (and (sensitivity-tagged-value? v0) (sensitivity-tagged-value? v))
	  (and (reverse-tagged-value? v0) (reverse-tagged-value? v))
	  (and (tagged-pair? v0)
	       (tagged-pair? v)
	       (equal-tags? (tagged-pair-tags v0) (tagged-pair-tags v))))
      ;; abstraction
      (map loop (aggregate-value-values v0) (aggregate-value-values v)))
     (else (internal-error)))))
  (c:return "r")))

(define (new-generate-unary-ad-definitions s descend? f g code)
 ;; here I am: The result of f might violate the syntactic constraints.
 ;; abstraction
 (let ((vs (second (all-sorted-unary-ad s descend?))))
  (for-each g vs)
  (map (lambda (v)
	(let ((v0 (f v)))
	 (c:specifier-function-definition
	  ;; The call to f might issue "might" warnings and might return an
	  ;; empty abstract value.
	  #t #f #f v0
	  (c:function-declarator*
	   (c:builtin-name code v)
	   (map (lambda (u)
		 (c:specifier-parameter
		  (name-unit-abstract-value u) (name-unit-code u)))
		(flatten (with-symbolic (lambda () (unitify v "x" #f #f))))))
	  (with-symbolic
	   (lambda ()
	    (new-generate-body (unitify v0 "r" #f #t)
			       (f (unitify v "x" #f #f))))))))
       vs)))

(define (new-generate-binary-ad-definitions
	 s descend? f? f f-inverse g aggregates-match? before h code)
 ;; here I am: The results of f, f-inverse, and g might violate the syntactic
 ;;            constraints.
 ;; abstraction
 (let ((vs (second (all-sorted-binary-ad
		    s descend? f? f f-inverse aggregates-match? before))))
  (for-each h vs)
  (map (lambda (v)
	(let ((v0 (g v)))
	 (c:specifier-function-definition
	  ;; The call to g might issue "might" warnings and might return an
	  ;; empty abstract value.
	  #t #f #f v0
	  (c:function-declarator*
	   (c:builtin-name code v)
	   (map (lambda (u)
		 (c:specifier-parameter
		  (name-unit-abstract-value u) (name-unit-code u)))
		(flatten (with-symbolic (lambda () (unitify v "x" #f #f))))))
	  (with-symbolic
	   (lambda ()
	    (new-generate-body (unitify v0 "r" #f #t)
			       (g (unitify v "x" #f #f))))))))
       vs)))

(define (new-generate-function-definitions
	 function-instances instances1-instances2)
 (for-each (lambda (instance)
	    (assert (function-instance? instance))
	    (set-function-instance-inline?! instance #f))
	   (second instances1-instances2))
 ;; abstraction
 (map (lambda (instance)
       (assert (function-instance? instance))
       (let* ((v1 (function-instance-v1 instance))
	      (v2 (function-instance-v2 instance))
	      (v0 (abstract-apply v1 v2)))
	(c:specifier-function-definition
	 #t #f #f v0
	 (c:function-declarator*
	  (c:function-name v1 v2 function-instances)
	  (append
	   (map (lambda (u)
		 (c:specifier-parameter
		  (name-unit-abstract-value u) (name-unit-code u)))
		(flatten (with-symbolic (lambda () (unitify v1 "x1" #f #f)))))
	   (map (lambda (u)
		 (c:specifier-parameter
		  (name-unit-abstract-value u) (name-unit-code u)))
		(flatten
		 (with-symbolic (lambda () (unitify v2 "x2" #f #f)))))))
	 (with-symbolic (lambda ()
			 (new-generate-body
			  (unitify v0 "r" #f #t)
			  (symbolic-apply
			   (unitify (function-instance-v1 instance) "x1" #f #f)
			   (unitify (function-instance-v2 instance) "x2" #f #f)
			   #t
			   function-instances)))))))
      (second instances1-instances2)))

(define (new-generate)
 (with-abstract
  (lambda ()
   (determine-void?!)
   (for-each-indexed (lambda (x i) (set-variable-index! x i)) *variables*)
   (let* ((function-instances (all-function-instances))
	  (widener-instances (all-widener-instances function-instances))
	  (vs1-vs2 (all-nested-abstract-values widener-instances))
	  (instances1-instances2
	   (all-instances1-instances2 function-instances)))
    (determine-void?!)
    (set! *frozen?* #t)
    (for-each-indexed
     (lambda (v i)
      (cond ((nonrecursive-closure? v) (set-nonrecursive-closure-c:index! v i))
	    ((recursive-closure? v) (set-recursive-closure-c:index! v i))
	    ((perturbation-tagged-value? v)
	     (set-perturbation-tagged-value-c:index! v i))
	    ((bundle? v) (set-bundle-c:index! v i))
	    ((sensitivity-tagged-value? v)
	     (set-sensitivity-tagged-value-c:index! v i))
	    ((reverse-tagged-value? v) (set-reverse-tagged-value-c:index! v i))
	    ((tagged-pair? v) (set-tagged-pair-c:index! v i))
	    ((union? v) (set-union-c:index! v i))
	    (else (set! *c:indices* (cons (cons v i) *c:indices*)))))
     (append (first vs1-vs2) (second vs1-vs2)))
    (for-each
     (lambda (v)
      (cond ((nonrecursive-closure? v) (set-nonrecursive-closure-boxed?! v #t))
	    ((recursive-closure? v) (set-recursive-closure-boxed?! v #t))
	    ((perturbation-tagged-value? v)
	     (set-perturbation-tagged-value-boxed?! v #t))
	    ((bundle? v) (set-bundle-boxed?! v #t))
	    ((sensitivity-tagged-value? v)
	     (set-sensitivity-tagged-value-boxed?! v #t))
	    ((reverse-tagged-value? v) (set-reverse-tagged-value-boxed?! v #t))
	    ((tagged-pair? v) (set-tagged-pair-boxed?! v #t))
	    ((union? v) (set-union-boxed?! v #t))
	    (else (internal-error))))
     (second vs1-vs2))
    ;; abstraction
    (list
     "#include <math.h>" #\newline
     "#include <stdio.h>" #\newline
     "#include <stdlib.h>" #\newline
     "#include <gc/gc.h>" #\newline
     "#define INLINE inline __attribute__ ((always_inline))" #\newline
     "#define NORETURN __attribute__ ((noreturn))" #\newline
     (new-generate-struct-and-union-declarations vs1-vs2)
     (c:specifier-function-declaration
      #t #t #t (empty-abstract-value)
      (c:function-declarator "panic"
			     (c:parameter "char" (c:pointer-declarator "x"))))
     (new-generate-unary-ad-declarations 'zero (lambda (v) #t) zero "zero")
     (new-generate-unary-ad-declarations
      'perturb
      (lambda (v)
       (and (not (perturbation-tagged-value? v))
	    (not (bundle? v))
	    (not (sensitivity-tagged-value? v))
	    (not (reverse-tagged-value? v))))
      perturb"perturb")
     (new-generate-unary-ad-declarations
      'unperturb
      (lambda (v)
       (and (perturbation-value? v) (not (perturbation-tagged-value? v))))
      unperturb "unperturb")
     (new-generate-unary-ad-declarations
      'primal (lambda (v) (and (forward-value? v) (not (bundle? v))))
      primal "primal")
     (new-generate-unary-ad-declarations
      'tangent (lambda (v) (and (forward-value? v) (not (bundle? v))))
      tangent "tangent")
     (new-generate-binary-ad-declarations
      'bundle
      (lambda (v)
       (and (not (perturbation-tagged-value? v))
	    (not (bundle? v))
	    (not (sensitivity-tagged-value? v))
	    (not (reverse-tagged-value? v))))
      perturbation-tagged-value? perturb unperturb bundle
      bundle-aggregates-match? bundle-before "bundle")
     (new-generate-unary-ad-declarations
      'sensitize
      (lambda (v)
       (and (not (perturbation-tagged-value? v))
	    (not (bundle? v))
	    (not (sensitivity-tagged-value? v))
	    (not (reverse-tagged-value? v))))
      sensitize "sensitize")
     (new-generate-unary-ad-declarations
      'unsensitize
      (lambda (v)
       (and (sensitivity-value? v) (not (sensitivity-tagged-value? v))))
      unsensitize "unsensitize")
     (new-generate-binary-ad-declarations
      'plus (lambda (v) #t) (lambda (v) #f) identity identity plus
      plus-aggregates-match? plus-before "plus")
     (new-generate-unary-ad-declarations
      '*j
      (lambda (v)
       (and (not (perturbation-tagged-value? v))
	    (not (bundle? v))
	    (not (sensitivity-tagged-value? v))
	    (not (reverse-tagged-value? v))))
      *j "starj")
     (new-generate-unary-ad-declarations
      '*j-inverse
      (lambda (v) (and (reverse-value? v) (not (reverse-tagged-value? v))))
      *j-inverse "starj_inverse")
     (new-generate-function-declarations
      function-instances instances1-instances2)
     (c:specifier-function-definition
      #t #t #t (empty-abstract-value)
      (c:function-declarator "panic"
			     (c:parameter "char" (c:pointer-declarator "x")))
      ;; abstraction
      "fputs(x,stderr);fputc('\\n',stderr);exit(EXIT_FAILURE);")
     (new-generate-unary-ad-definitions
      'zero (lambda (v) #t) zero
      (lambda (v)
       (cond
	((union? v) (set-union-inline-zero?! v #f))
	((nonrecursive-closure? v)
	 (set-nonrecursive-closure-inline-zero?! v #f))
	((recursive-closure? v) (set-recursive-closure-inline-zero?! v #f))
	((perturbation-tagged-value? v)
	 (set-perturbation-tagged-value-inline-zero?! v #f))
	((bundle? v) (set-bundle-inline-zero?! v #f))
	((sensitivity-tagged-value? v)
	 (set-sensitivity-tagged-value-inline-zero?! v #f))
	((reverse-tagged-value? v)
	 (set-reverse-tagged-value-inline-zero?! v #f))
	((tagged-pair? v) (set-tagged-pair-inline-zero?! v #f))
	(else (internal-error))))
      "zero")
     (new-generate-unary-ad-definitions
      'perturb
      (lambda (v)
       (and (not (perturbation-tagged-value? v))
	    (not (bundle? v))
	    (not (sensitivity-tagged-value? v))
	    (not (reverse-tagged-value? v))))
      perturb
      (lambda (v)
       (cond
	((union? v) (set-union-inline-perturb?! v #f))
	((nonrecursive-closure? v)
	 (set-nonrecursive-closure-inline-perturb?! v #f))
	((recursive-closure? v) (set-recursive-closure-inline-perturb?! v #f))
	((perturbation-tagged-value? v)
	 (set-perturbation-tagged-value-inline-perturb?! v #f))
	((bundle? v) (set-bundle-inline-perturb?! v #f))
	((sensitivity-tagged-value? v)
	 (set-sensitivity-tagged-value-inline-perturb?! v #f))
	((reverse-tagged-value? v)
	 (set-reverse-tagged-value-inline-perturb?! v #f))
	((tagged-pair? v) (set-tagged-pair-inline-perturb?! v #f))
	(else (internal-error))))
      "perturb")
     (new-generate-unary-ad-definitions
      'unperturb
      (lambda (v)
       (and (perturbation-value? v) (not (perturbation-tagged-value? v))))
      unperturb
      (lambda (v)
       (cond ((union? v) (set-union-inline-unperturb?! v #f))
	     ((nonrecursive-closure? v)
	      (set-nonrecursive-closure-inline-unperturb?! v #f))
	     ((recursive-closure? v)
	      (set-recursive-closure-inline-unperturb?! v #f))
	     ((perturbation-tagged-value? v)
	      (set-perturbation-tagged-value-inline-unperturb?! v #f))
	     ((bundle? v) (set-bundle-inline-unperturb?! v #f))
	     ((sensitivity-tagged-value? v)
	      (set-sensitivity-tagged-value-inline-unperturb?! v #f))
	     ((reverse-tagged-value? v)
	      (set-reverse-tagged-value-inline-unperturb?! v #f))
	     ((tagged-pair? v) (set-tagged-pair-inline-unperturb?! v #f))
	     (else (internal-error))))
      "unperturb")
     (new-generate-unary-ad-definitions
      'primal (lambda (v) (and (forward-value? v) (not (bundle? v))))
      primal
      (lambda (v)
       (cond
	((union? v) (set-union-inline-primal?! v #f))
	((nonrecursive-closure? v)
	 (set-nonrecursive-closure-inline-primal?! v #f))
	((recursive-closure? v) (set-recursive-closure-inline-primal?! v #f))
	((perturbation-tagged-value? v)
	 (set-perturbation-tagged-value-inline-primal?! v #f))
	((bundle? v) (set-bundle-inline-primal?! v #f))
	((sensitivity-tagged-value? v)
	 (set-sensitivity-tagged-value-inline-primal?! v #f))
	((reverse-tagged-value? v)
	 (set-reverse-tagged-value-inline-primal?! v #f))
	((tagged-pair? v) (set-tagged-pair-inline-primal?! v #f))
	(else (internal-error))))
      "primal")
     (new-generate-unary-ad-definitions
      'tangent (lambda (v) (and (forward-value? v) (not (bundle? v))))
      tangent
      (lambda (v)
       (cond
	((union? v) (set-union-inline-tangent?! v #f))
	((nonrecursive-closure? v)
	 (set-nonrecursive-closure-inline-tangent?! v #f))
	((recursive-closure? v) (set-recursive-closure-inline-tangent?! v #f))
	((perturbation-tagged-value? v)
	 (set-perturbation-tagged-value-inline-tangent?! v #f))
	((bundle? v) (set-bundle-inline-tangent?! v #f))
	((sensitivity-tagged-value? v)
	 (set-sensitivity-tagged-value-inline-tangent?! v #f))
	((reverse-tagged-value? v)
	 (set-reverse-tagged-value-inline-tangent?! v #f))
	((tagged-pair? v) (set-tagged-pair-inline-tangent?! v #f))
	(else (internal-error))))
      "tangent")
     (new-generate-binary-ad-definitions
      'bundle
      (lambda (v)
       (and (not (perturbation-tagged-value? v))
	    (not (bundle? v))
	    (not (sensitivity-tagged-value? v))
	    (not (reverse-tagged-value? v))))
      perturbation-tagged-value? perturb unperturb bundle
      bundle-aggregates-match? bundle-before
      (lambda (v)
       (cond ((union? v) (set-union-inline-bundle?! v #f))
	     ((tagged-pair? v) (set-tagged-pair-inline-bundle?! v #f))
	     (else (internal-error))))
      "bundle")
     (new-generate-unary-ad-definitions
      'sensitize
      (lambda (v)
       (and (not (perturbation-tagged-value? v))
	    (not (bundle? v))
	    (not (sensitivity-tagged-value? v))
	    (not (reverse-tagged-value? v))))
      sensitize
      (lambda (v)
       (cond ((union? v) (set-union-inline-sensitize?! v #f))
	     ((nonrecursive-closure? v)
	      (set-nonrecursive-closure-inline-sensitize?! v #f))
	     ((recursive-closure? v)
	      (set-recursive-closure-inline-sensitize?! v #f))
	     ((perturbation-tagged-value? v)
	      (set-perturbation-tagged-value-inline-sensitize?! v #f))
	     ((bundle? v) (set-bundle-inline-sensitize?! v #f))
	     ((sensitivity-tagged-value? v)
	      (set-sensitivity-tagged-value-inline-sensitize?! v #f))
	     ((reverse-tagged-value? v)
	      (set-reverse-tagged-value-inline-sensitize?! v #f))
	     ((tagged-pair? v) (set-tagged-pair-inline-sensitize?! v #f))
	     (else (internal-error))))
      "sensitize")
     (new-generate-unary-ad-definitions
      'unsensitize
      (lambda (v)
       (and (sensitivity-value? v) (not (sensitivity-tagged-value? v))))
      unsensitize
      (lambda (v)
       (cond ((union? v) (set-union-inline-unsensitize?! v #f))
	     ((nonrecursive-closure? v)
	      (set-nonrecursive-closure-inline-unsensitize?! v #f))
	     ((recursive-closure? v)
	      (set-recursive-closure-inline-unsensitize?! v #f))
	     ((perturbation-tagged-value? v)
	      (set-perturbation-tagged-value-inline-unsensitize?! v #f))
	     ((bundle? v) (set-bundle-inline-unsensitize?! v #f))
	     ((sensitivity-tagged-value? v)
	      (set-sensitivity-tagged-value-inline-unsensitize?! v #f))
	     ((reverse-tagged-value? v)
	      (set-reverse-tagged-value-inline-unsensitize?! v #f))
	     ((tagged-pair? v) (set-tagged-pair-inline-unsensitize?! v #f))
	     (else (internal-error))))
      "unsensitize")
     (new-generate-binary-ad-definitions
      'plus (lambda (v) #t) (lambda (v) #f) identity identity plus
      plus-aggregates-match? plus-before
      (lambda (v)
       (cond ((union? v) (set-union-inline-plus?! v #f))
	     ((tagged-pair? v) (set-tagged-pair-inline-plus?! v #f))
	     (else (internal-error))))
      "plus")
     (new-generate-unary-ad-definitions
      '*j
      (lambda (v)
       (and (not (perturbation-tagged-value? v))
	    (not (bundle? v))
	    (not (sensitivity-tagged-value? v))
	    (not (reverse-tagged-value? v))))
      *j
      (lambda (v)
       (cond
	((union? v) (set-union-inline-*j?! v #f))
	((nonrecursive-closure? v) (set-nonrecursive-closure-inline-*j?! v #f))
	((recursive-closure? v) (set-recursive-closure-inline-*j?! v #f))
	((perturbation-tagged-value? v)
	 (set-perturbation-tagged-value-inline-*j?! v #f))
	((bundle? v) (set-bundle-inline-*j?! v #f))
	((sensitivity-tagged-value? v)
	 (set-sensitivity-tagged-value-inline-*j?! v #f))
	((reverse-tagged-value? v) (set-reverse-tagged-value-inline-*j?! v #f))
	((tagged-pair? v) (set-tagged-pair-inline-*j?! v #f))
	(else (internal-error))))
      "starj")
     (new-generate-unary-ad-definitions
      '*j-inverse
      (lambda (v) (and (reverse-value? v) (not (reverse-tagged-value? v))))
      *j-inverse
      (lambda (v)
       (cond ((union? v) (set-union-inline-*j-inverse?! v #f))
	     ((nonrecursive-closure? v)
	      (set-nonrecursive-closure-inline-*j-inverse?! v #f))
	     ((recursive-closure? v)
	      (set-recursive-closure-inline-*j-inverse?! v #f))
	     ((perturbation-tagged-value? v)
	      (set-perturbation-tagged-value-inline-*j-inverse?! v #f))
	     ((bundle? v) (set-bundle-inline-*j-inverse?! v #f))
	     ((sensitivity-tagged-value? v)
	      (set-sensitivity-tagged-value-inline-*j-inverse?! v #f))
	     ((reverse-tagged-value? v)
	      (set-reverse-tagged-value-inline-*j-inverse?! v #f))
	     ((tagged-pair? v) (set-tagged-pair-inline-*j-inverse?! v #f))
	     (else (internal-error))))
      "starj_inverse")
     (new-generate-function-definitions
      function-instances instances1-instances2))))))

;;; Serialization

(define (all-subobjects object)
 (let ((objects '()))
  (let loop ((object object))
   (cond ((primitive-procedure? object) #f)
	 ((string? object)
	  (unless (memq object objects) (set! objects (cons object objects))))
	 ((pair? object)
	  (unless (memq object objects)
	   (set! objects (cons object objects))
	   (loop (car object))
	   (loop (cdr object))))
	 ((vector? object)
	  (unless (memq object objects)
	   (set! objects (cons object objects))
	   (for-each-vector loop object)))))
  objects))

(define (serialize-object object objects)
 (cond ((primitive-procedure? object)
	`(primitive-procedure
	  ,(positionq object (map value-binding-value *value-bindings*))))
       ((or (null? object)
	    (boolean? object)
	    (char? object)
	    (and (number? object) (exact? object))
	    (symbol? object))
	object)
       ((and (number? object) (inexact? object))
	`(double ,(double-part object 0)
		 ,(double-part object 1)
		 ,(double-part object 2)
		 ,(double-part object 3)))
       ((or (string? object) (pair? object) (vector? object))
	`(table ,(positionq object objects)))
       (else (internal-error "Cannot serialize this object"))))

(define (serialize object)
 (let ((objects (all-subobjects object)))
  (cons
   (serialize-object object objects)
   (map (lambda (object)
	 (cond ((primitive-procedure? object) (internal-error))
	       ((string? object) object)
	       ((pair? object)
		(cons (serialize-object (car object) objects)
		      (serialize-object (cdr object) objects)))
	       ((vector? object)
		(map-vector (lambda (object) (serialize-object object objects))
			    object))
	       (else (internal-error))))
	objects))))

(define (unserialize-object object objects)
 (cond ((or (null? object)
	    (boolean? object)
	    (char? object)
	    (and (number? object) (exact? object))
	    (symbol? object))
	object)
       ((pair? object)
	(case (first object)
	 ((primitive-procedure)
	  (value-binding-value (list-ref *value-bindings* (second object))))
	 ((double)
	  (make-double
	   (second object) (third object) (fourth object) (fifth object)))
	 ((table) (list-ref objects (second object)))
	 (else (internal-error "Cannot unserialize this object"))))
       (else (internal-error "Cannot unserialize this object"))))

(define (unserialize objects)
 (for-each
  (lambda (object)
   (cond
    ((string? object) #f)
    ((pair? object)
     (set-car! object (unserialize-object (car object) (rest objects)))
     (set-cdr! object (unserialize-object (cdr object) (rest objects))))
    ((vector? object)
     (for-each-n
      (lambda (i)
       (vector-set!
	object i (unserialize-object (vector-ref object i) (rest objects))))
      (vector-length object)))
    (else (internal-error))))
  (rest objects))
 (unserialize-object (first objects) (rest objects)))

(define (write-ebs-to-file bs pathname)
 (call-with-output-file (replace-extension pathname "ebs")
  (lambda (port) (write (serialize bs) port) (newline port))))

(define (read-ebs-from-file pathname)
 (unserialize (read-object-from-file (replace-extension pathname "ebs"))))

;;; Primitives

(define (divide x1 x2)
 ;; An approximation of IEEE since Scheme->C hides it. Doesn't handle positive
 ;; vs. negative zero x2.
 (if (zero? x2)
     (cond ((positive? x1) infinity)
	   ((negative? x1) minus-infinity)
	   ;; Both zeros and nan.
	   (else nan))
     (/ x1 x2)))

(define (concrete-read-real v)
 (unless (vlad-empty-list? v)
  (run-time-error "Argument is not an equivalent value for '()" v))
 (let ((v (read)))
  (unless (real? v) (run-time-error "Value read is not real" v))
  v))

(define (concrete-unary f) (lambda (v) (f v)))

(define (concrete-ad f) f)

(define (concrete-unary-predicate f)
 (lambda (v) (if (f v) (vlad-true) (vlad-false))))

(define (concrete-unary-real f s)
 (lambda (v)
  (unless (vlad-real? v)
   (run-time-error (format #f "Argument to ~a is invalid" s) v))
  (f v)))

(define (concrete-unary-real-predicate f s)
 (lambda (v)
  (unless (vlad-real? v)
   (run-time-error (format #f "Argument to ~a is invalid" s) v))
  (if (f v) (vlad-true) (vlad-false))))

(define (concrete-binary-real f s)
 (lambda (v)
  (unless (vlad-pair? v)
   (run-time-error (format #f "Argument to ~a is invalid" s) v))
  (let ((v1 (vlad-car v)) (v2 (vlad-cdr v)))
   (unless (and (vlad-real? v1) (vlad-real? v2))
    (run-time-error (format #f "Argument to ~a is invalid" s) v))
   (f v1 v2))))

(define (concrete-binary-real-predicate f s)
 (lambda (v)
  (unless (vlad-pair? v)
   (run-time-error (format #f "Argument to ~a is invalid" s) v))
  (let ((v1 (vlad-car v)) (v2 (vlad-cdr v)))
   (unless (and (vlad-real? v1) (vlad-real? v2))
    (run-time-error (format #f "Argument to ~a is invalid" s) v))
   (if (f v1 v2) (vlad-true) (vlad-false)))))

(define (concrete-if-procedure v)
 (unless (vlad-pair? v)
  (run-time-error "Argument to if-procedure is invalid" v))
 (let ((v23 (vlad-cdr v)))
  (unless (vlad-pair? v23)
   (run-time-error "Argument to if-procedure is invalid" v))
  (concrete-apply ((if (vlad-false? (vlad-car v)) vlad-cdr vlad-car) v23)
		  (vlad-empty-list))))

(define (abstract-read-real v)
 (if (vlad-empty-list? v)
     (abstract-real)
     (compile-time-warning
      "Argument might not be an equivalent value for '()" v)))

(define (abstract-unary f) (lambda (v) (map-union f v)))

(define (abstract-ad f) f)

(define (abstract-unary-predicate f)
 (lambda (v) (map-union (lambda (u) (if (f u) (vlad-true) (vlad-false))) v)))

(define (abstract-unary-real f s)
 (lambda (v)
  (map-union (lambda (u)
	      (if (vlad-real? u)
		  (if (real? u) (f u) (abstract-real))
		  (compile-time-warning
		   (format #f "Argument to ~a might be invalid" s) u)))
	     v)))

(define (abstract-unary-real-predicate f s)
 (lambda (v)
  (map-union
   (lambda (u)
    (if (vlad-real? u)
	(if (real? u) (if (f u) (vlad-true) (vlad-false)) (abstract-boolean))
	(compile-time-warning
	 (format #f "Argument to ~a might be invalid" s) u)))
   v)))

(define (abstract-binary-real f s)
 (lambda (v)
  (map-union
   (lambda (u)
    (if (vlad-pair? u)
	(cross-union
	 (lambda (u1 u2)
	  (if (and (vlad-real? u1) (vlad-real? u2))
	      ;; needs work: This may be imprecise for *, /, and atan.
	      (if (and (real? u1) (real? u2)) (f u1 u2) (abstract-real))
	      (compile-time-warning
	       (format #f "Argument to ~a might be invalid" s) u)))
	 (vlad-car u)
	 (vlad-cdr u))
	(compile-time-warning
	 (format #f "Argument to ~a might be invalid" s) u)))
   v)))

(define (abstract-binary-real-predicate f s)
 (lambda (v)
  (map-union
   (lambda (u)
    (if (vlad-pair? u)
	(cross-union (lambda (u1 u2)
		      (if (and (vlad-real? u1) (vlad-real? u2))
			  (if (and (real? u1) (real? u2))
			      (if (f u1 u2) (vlad-true) (vlad-false))
			      (abstract-boolean))
			  (compile-time-warning
			   (format #f "Argument to ~a might be invalid" s) u)))
		     (vlad-car u)
		     (vlad-cdr u))
	(compile-time-warning
	 (format #f "Argument to ~a might be invalid" s) u)))
   v)))

(define (abstract-if-procedure v)
 (map-union
  (lambda (u)
   (if (vlad-pair? u)
       (cross-union
	(lambda (u1 u23)
	 (if (vlad-pair? u23)
	     (cross-union (lambda (u2 u3)
			   (abstract-apply (if (vlad-false? u1) u3 u2)
					   (vlad-empty-list)))
			  (vlad-car u23)
			  (vlad-cdr u23))
	     (compile-time-warning
	      "Argument to if-procedure might be invalid" u)))
	(vlad-car u)
	(vlad-cdr u))
       (compile-time-warning "Argument to if-procedure might be invalid" u)))
  v))

(define (symbolic-read-real v)
 (if (vlad-empty-list? v)
     (make-read-real-unit v)
     (new-panic-unit "Argument might not be an equivalent value for '()")))

(define (symbolic-unary f g)
 (lambda (v function-instances)
  (cond ((unit? v)
	 (if (union? (unit-abstract-value v))
	     ((symbolic-unary f g) (unroll v))
	     (g v)))
	((union? v)
	 (create-tagged-union
	  (union-tag v) (map (symbolic-unary f g) (get-union-values v))))
	(else (f v)))))

(define (symbolic-ad f) (lambda (v function-instances) (f v)))

(define (symbolic-unary-predicate f)
 (lambda (v function-instances)
  (cond
   ((unit? v)
    (if (union? (unit-abstract-value v))
	((symbolic-unary-predicate f) (unroll v) function-instances)
	(if (f (unit-abstract-value v)) (vlad-true) (vlad-false))))
   ((union? v)
    (create-tagged-union
     (union-tag v)
     (map (lambda (v) ((symbolic-unary-predicate f) v function-instances))
	  (get-union-values v))))
   (else (if (f v) (vlad-true) (vlad-false))))))

(define (symbolic-unary-real f g s)
 (lambda (v function-instances)
  (cond
   ((unit? v)
    (cond ((union? (unit-abstract-value v))
	   ((symbolic-unary-real f g s) (unroll v) function-instances))
	  ((vlad-real? (unit-abstract-value v)) (g v))
	  (else (new-panic-unit (format #f "Argument to ~a is invalid" s)))))
   ((union? v)
    (create-tagged-union
     (union-tag v)
     (map (lambda (v) ((symbolic-unary-real f g s) v function-instances))
	  (get-union-values v))))
   ((vlad-real? v)
    (assert (real? v))
    (f v))
   (else (new-panic-unit (format #f "Argument to ~a is invalid" s))))))

(define (symbolic-unary-real-predicate f g s)
 (lambda (v function-instances)
  (cond
   ((unit? v)
    (cond
     ((union? (unit-abstract-value v))
      ((symbolic-unary-real-predicate f g s) (unroll v) function-instances))
     ((vlad-real? (unit-abstract-value v)) (g v))
     (else (new-panic-unit (format #f "Argument to ~a is invalid" s)))))
   ((union? v)
    (create-tagged-union
     (union-tag v)
     (map (lambda (v)
	   ((symbolic-unary-real-predicate f g s) v function-instances))
	  (get-union-values v))))
   ((vlad-real? v)
    (assert (real? v))
    (if (f v) (vlad-true) (vlad-false)))
   (else (new-panic-unit (format #f "Argument to ~a is invalid" s))))))

(define (symbolic-binary-real1 f g s v1 v2)
 (cond
  ((and (unit? v1) (union? (unit-abstract-value v1)))
   (symbolic-binary-real1 f g s (unroll v1) v2))
  ((and (unit? v2) (union? (unit-abstract-value v2)))
   (symbolic-binary-real1 f g s v1 (unroll v2)))
  ((or (and (unit? v1)
	    (vlad-real? (unit-abstract-value v1))
	    (unit? v2)
	    (vlad-real? (unit-abstract-value v2)))
       (and (unit? v1)
	    (vlad-real? (unit-abstract-value v1))
	    (vlad-real? v2))
       (and (vlad-real? v1)
	    (unit? v2)
	    (vlad-real? (unit-abstract-value v2))))
   (assert (and (not (abstract-real? v1))
		(not (abstract-real? v2))))
   ;; needs work: This may be imprecise for *, /, and atan.
   (g v1 v2))
  ((union? v1)
   (create-tagged-union (union-tag v1)
			(map (lambda (u1) (symbolic-binary-real1 f g s u1 v2))
			     (get-union-values v1))))
  ((union? v2)
   (create-tagged-union (union-tag v2)
			(map (lambda (u2) (symbolic-binary-real1 f g s v1 u2))
			     (get-union-values v2))))
  ((and (vlad-real? v1) (vlad-real? v2))
   (assert (and (real? v1) (real? v2)))
   (f v1 v2))
  (else (new-panic-unit (format #f "Argument to ~a is invalid" s)))))

(define (symbolic-binary-real f g s)
 (lambda (v function-instances)
  (cond
   ((unit? v)
    (if (or (union? (unit-abstract-value v))
	    (vlad-pair? (unit-abstract-value v)))
	((symbolic-binary-real f g s) (unroll v) function-instances)
	(new-panic-unit (format #f "Argument to ~a is invalid" s))))
   ((union? v)
    (create-tagged-union
     (union-tag v)
     (map (lambda (v) ((symbolic-binary-real f g s) v function-instances))
	  (get-union-values v))))
   ((vlad-pair? v) (symbolic-binary-real1 f g s (vlad-car v) (vlad-cdr v)))
   (else (new-panic-unit (format #f "Argument to ~a is invalid" s))))))

(define (symbolic-binary-real-predicate1 f g s v1 v2)
 (cond
  ((and (unit? v1) (union? (unit-abstract-value v1)))
   (symbolic-binary-real-predicate1 f g s (unroll v1) v2))
  ((and (unit? v2) (union? (unit-abstract-value v2)))
   (symbolic-binary-real-predicate1 f g s v1 (unroll v2)))
  ((or (and (unit? v1)
	    (vlad-real? (unit-abstract-value v1))
	    (unit? v2)
	    (vlad-real? (unit-abstract-value v2)))
       (and (unit? v1)
	    (vlad-real? (unit-abstract-value v1))
	    (vlad-real? v2))
       (and (vlad-real? v1)
	    (unit? v2)
	    (vlad-real? (unit-abstract-value v2))))
   (assert (and (not (abstract-real? v1))
		(not (abstract-real? v2))))
   (g v1 v2))
  ((union? v1)
   (create-tagged-union (union-tag v1)
			(map (lambda (u1) (symbolic-binary-real1 f g s u1 v2))
			     (get-union-values v1))))
  ((union? v2)
   (create-tagged-union (union-tag v2)
			(map (lambda (u2) (symbolic-binary-real1 f g s v1 u2))
			     (get-union-values v2))))
  ((and (vlad-real? v1) (vlad-real? v2))
   (assert (and (real? v1) (real? v2)))
   (if (f v1 v2) (vlad-true) (vlad-false)))
  (else (new-panic-unit (format #f "Argument to ~a is invalid" s)))))

(define (symbolic-binary-real-predicate f g s)
 (lambda (v function-instances)
  (cond
   ((unit? v)
    (if (or (union? (unit-abstract-value v))
	    (vlad-pair? (unit-abstract-value v)))
	((symbolic-binary-real-predicate f g s) (unroll v) function-instances)
	(new-panic-unit (format #f "Argument to ~a is invalid" s))))
   ((union? v)
    (create-tagged-union
     (union-tag v)
     (map (lambda (v)
	   ((symbolic-binary-real-predicate f g s) v function-instances))
	  (get-union-values v))))
   ((vlad-pair? v)
    (symbolic-binary-real-predicate1 f g s (vlad-car v) (vlad-cdr v)))
   (else (new-panic-unit (format #f "Argument to ~a is invalid" s))))))

(define (symbolic-if-procedure2 v1 v2 function-instances)
 (cond ((unit? v2)
	(if (or (union? (unit-abstract-value v1))
		(vlad-pair? (unit-abstract-value v1)))
	    (symbolic-if-procedure2 v1 (unroll v2) function-instances)
	    (new-panic-unit "Argument to if-procedure is invalid")))
       ((union? v2)
	(create-tagged-union
	 (union-tag v2)
	 (map (lambda (u2) (symbolic-if-procedure2 v1 u2 function-instances))
	      (get-union-values v2))))
       ((vlad-pair? v2)
	(symbolic-apply ((if (vlad-false? v1) vlad-cdr vlad-car) v2)
			(vlad-empty-list)
			#f
			function-instances))
       (else (new-panic-unit "Argument to if-procedure is invalid"))))

(define (symbolic-if-procedure1 v1 v2 function-instances)
 (cond ((unit? v1)
	(if (union? (unit-abstract-value v1))
	    (symbolic-if-procedure1 (unroll v1) v2 function-instances)
	    (symbolic-if-procedure2 v1 v2 function-instances)))
       ((union? v1)
	(create-tagged-union
	 (union-tag v1)
	 (map (lambda (u1) (symbolic-if-procedure1 u1 v2 function-instances))
	      (get-union-values v1))))
       (else (symbolic-if-procedure2 v1 v2 function-instances))))

(define (symbolic-if-procedure v function-instances)
 (cond ((unit? v)
	(if (or (union? (unit-abstract-value v))
		(vlad-pair? (unit-abstract-value v)))
	    (symbolic-if-procedure (unroll v) function-instances)
	    (new-panic-unit "Argument to if-procedure is invalid")))
       ((union? v)
	(create-tagged-union
	 (union-tag v)
	 (map (lambda (v) (symbolic-if-procedure v function-instances))
	      (get-union-values v) function-instances)))
       ((vlad-pair? v)
	(symbolic-if-procedure1 (vlad-car v) (vlad-cdr v) function-instances))
       (else (new-panic-unit "Argument to if-procedure is invalid"))))

(define (define-primitive-procedure
	 x concrete abstract symbolic generator forward reverse)
 (set! *value-bindings*
       (cons
	(make-value-binding
	 (new-variable x)
	 (make-primitive-procedure
	  x concrete abstract symbolic generator forward reverse 0))
	*value-bindings*)))

(define (constant-unconvert e)
 ;; This is particular to the way the forward and reverse transforms of the
 ;; basis are written. It doesn't handle lexical scope shadowing.
 (cond ((constant-expression? e) e)
       ((variable-access-expression? e)
	(let ((b (find-if (lambda (b)
			   (variable=? (variable-access-expression-variable e)
				       (value-binding-variable b)))
			  *value-bindings*)))
	 (if b (new-constant-expression (value-binding-value b)) e)))
       ((lambda-expression? e)
	(new-lambda-expression
	 (lambda-expression-parameter e)
	 (constant-unconvert (lambda-expression-body e))))
       ((application? e)
	(new-application (constant-unconvert (application-callee e))
			 (constant-unconvert (application-argument e))))
       ((letrec-expression? e)
	(new-letrec-expression
	 (letrec-expression-procedure-variables e)
	 (map constant-unconvert (letrec-expression-lambda-expressions e))
	 (constant-unconvert (letrec-expression-body e))))
       ((cons-expression? e)
	(new-cons-expression (cons-expression-tags e)
			     (constant-unconvert (cons-expression-car e))
			     (constant-unconvert (cons-expression-cdr e))))
       (else (internal-error))))

(define (evaluate-in-top-level-environment e)
 (let ((wizard? *wizard?*))
  (set! *wizard?* #t)
  (syntax-check-expression! e)
  (set! *wizard?* wizard?))
 (new-nonrecursive-closure
  '()
  (anf-convert-lambda-expression
   (alpha-convert (constant-unconvert (internalize-expression e))))))

(define (initialize-forwards-and-reverses!)
 (for-each (lambda (b)
	    (let ((v (value-binding-value b)))
	     (let ((v-forward (evaluate-in-top-level-environment
			       (primitive-procedure-forward v))))
	      (set-primitive-procedure-forward! v v-forward)
	      (set-nonrecursive-closure-primal-cache! v-forward v)
	      (set-nonrecursive-closure-tangent-cache!
	       v-forward (new-perturbation-tagged-value v)))
	     (let ((v-reverse (evaluate-in-top-level-environment
			       (primitive-procedure-reverse v))))
	      (set-primitive-procedure-reverse! v v-reverse)
	      (set-nonrecursive-closure-*j-inverse-cache! v-reverse v))))
	   *value-bindings*))

(define (initialize-basis!)
 (set! *empty-abstract-value* (create-union '()))
 (when *interned?*
  (assert (not *frozen?*))
  (set! *unions* (cons (empty-abstract-value) *unions*))
  (set-union-canonize-cache! *empty-abstract-value* *empty-abstract-value*)
  (set-union-intern-cache! *empty-abstract-value* *empty-abstract-value*))
 (set! *abstract-boolean* (new-union (list (vlad-true) (vlad-false))))
 (define-primitive-procedure '+
  (concrete-binary-real + "+")
  (abstract-binary-real + "+")
  (symbolic-binary-real + new-+-unit "+")
  (lambda (v) (c:builtin-name "add" v))
  '(lambda ((forward x))
    (let (((cons x1 x2) (primal (forward x)))
	  ((cons x1-unperturbed x2-unperturbed)
	   (unperturb (tangent (forward x)))))
     (bundle (+ x1 x2) (perturb (+ x1-unperturbed x2-unperturbed)))))
  '(lambda ((reverse x))
    (let (((cons x1 x2) (*j-inverse (reverse x))))
     (cons (*j (+ x1 x2))
	   (lambda ((sensitivity y))
	    (sensitize (cons +
			     (cons (unsensitize (sensitivity y))
				   (unsensitize (sensitivity y))))))))))
 (define-primitive-procedure '-
  (concrete-binary-real - "-")
  (abstract-binary-real - "-")
  (symbolic-binary-real - new---unit "-")
  (lambda (v) (c:builtin-name "minus" v))
  '(lambda ((forward x))
    (let (((cons x1 x2) (primal (forward x)))
	  ((cons x1-unperturbed x2-unperturbed)
	   (unperturb (tangent (forward x)))))
     (bundle (- x1 x2) (perturb (- x1-unperturbed x2-unperturbed)))))
  (if *imprecise-inexacts?*
      '(lambda ((reverse x))
	(let (((cons x1 x2) (*j-inverse (reverse x))))
	 (cons (*j (- x1 x2))
	       (lambda ((sensitivity y))
		(sensitize
		 (cons -
		       (cons (unsensitize (sensitivity y))
			     (- 0.0 (unsensitize (sensitivity y))))))))))
      '(lambda ((reverse x))
	(let (((cons x1 x2) (*j-inverse (reverse x))))
	 (cons (*j (- x1 x2))
	       (lambda ((sensitivity y))
		(sensitize
		 (cons -
		       (cons (unsensitize (sensitivity y))
			     (- 0 (unsensitize (sensitivity y))))))))))))
 (define-primitive-procedure '*
  (concrete-binary-real * "*")
  (abstract-binary-real * "*")
  (symbolic-binary-real * new-*-unit "*")
  (lambda (v) (c:builtin-name "times" v))
  '(lambda ((forward x))
    (let (((cons x1 x2) (primal (forward x)))
	  ((cons x1-unperturbed x2-unperturbed)
	   (unperturb (tangent (forward x)))))
     (bundle (* x1 x2)
	     (perturb (+ (* x2 x1-unperturbed) (* x1 x2-unperturbed))))))
  '(lambda ((reverse x))
    (let (((cons x1 x2) (*j-inverse (reverse x))))
     (cons (*j (* x1 x2))
	   (lambda ((sensitivity y))
	    (sensitize
	     (cons *
		   (cons (* x2 (unsensitize (sensitivity y)))
			 (* x1 (unsensitize (sensitivity y)))))))))))
 (define-primitive-procedure '/
  (concrete-binary-real divide "/")
  (abstract-binary-real divide "/")
  (symbolic-binary-real divide new-/-unit "/")
  (lambda (v) (c:builtin-name "divide" v))
  '(lambda ((forward x))
    (let (((cons x1 x2) (primal (forward x)))
	  ((cons x1-unperturbed x2-unperturbed)
	   (unperturb (tangent (forward x)))))
     (bundle (/ x1 x2)
	     (perturb (/ (- (* x2 x1-unperturbed) (* x1 x2-unperturbed))
			 (* x2 x2))))))
  (if *imprecise-inexacts?*
      '(lambda ((reverse x))
	(let (((cons x1 x2) (*j-inverse (reverse x))))
	 (cons (*j (/ x1 x2))
	       (lambda ((sensitivity y))
		(sensitize
		 (cons /
		       (cons (/ (unsensitize (sensitivity y)) x2)
			     (- 0.0
				(/ (* x1 (unsensitize (sensitivity y)))
				   (* x2 x2))))))))))
      '(lambda ((reverse x))
	(let (((cons x1 x2) (*j-inverse (reverse x))))
	 (cons (*j (/ x1 x2))
	       (lambda ((sensitivity y))
		(sensitize
		 (cons /
		       (cons (/ (unsensitize (sensitivity y)) x2)
			     (- 0
				(/ (* x1 (unsensitize (sensitivity y)))
				   (* x2 x2))))))))))))
 (define-primitive-procedure 'sqrt
  (concrete-unary-real sqrt "sqrt")
  (abstract-unary-real sqrt "sqrt")
  (symbolic-unary-real sqrt new-sqrt-unit "sqrt")
  (lambda (v) "sqrt")
  '(lambda ((forward x))
    (let ((x (primal (forward x))) ((perturbation x) (tangent (forward x))))
     (bundle
      (sqrt x)
      (perturb (/ (unperturb (perturbation x)) (+ (sqrt x) (sqrt x)))))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons
      (*j (sqrt x))
      (lambda ((sensitivity y))
       (sensitize
	(cons sqrt
	      (/ (unsensitize (sensitivity y)) (+ (sqrt x) (sqrt x))))))))))
 (define-primitive-procedure 'exp
  (concrete-unary-real exp "exp")
  (abstract-unary-real exp "exp")
  (symbolic-unary-real exp new-exp-unit "exp")
  (lambda (v) "exp")
  '(lambda ((forward x))
    (let* ((x (primal (forward x)))
	   ((perturbation x) (tangent (forward x)))
	   ;; debugging
	   (foo (exp x)))
     (bundle foo (perturb (* foo (unperturb (perturbation x)))))))
  '(lambda ((reverse x))
    (let* ((x (*j-inverse (reverse x)))
	   ;; debugging
	   (foo (exp x)))
     (cons (*j foo)
	   (lambda ((sensitivity y))
	    (sensitize (cons exp (* foo (unsensitize (sensitivity y))))))))))
 (define-primitive-procedure 'log
  (concrete-unary-real log "log")
  (abstract-unary-real log "log")
  (symbolic-unary-real log new-log-unit "log")
  (lambda (v) "log")
  '(lambda ((forward x))
    (let ((x (primal (forward x))) ((perturbation x) (tangent (forward x))))
     (bundle (log x) (perturb (/ (unperturb (perturbation x)) x)))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons (*j (log x))
	   (lambda ((sensitivity y))
	    (sensitize (cons log (/ (unsensitize (sensitivity y)) x))))))))
 (define-primitive-procedure 'sin
  (concrete-unary-real sin "sin")
  (abstract-unary-real sin "sin")
  (symbolic-unary-real sin new-sin-unit "sin")
  (lambda (v) "sin")
  '(lambda ((forward x))
    (let ((x (primal (forward x))) ((perturbation x) (tangent (forward x))))
     (bundle (sin x) (perturb (* (cos x) (unperturb (perturbation x)))))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons
      (*j (sin x))
      (lambda ((sensitivity y))
       (sensitize (cons sin (* (cos x) (unsensitize (sensitivity y))))))))))
 (define-primitive-procedure 'cos
  (concrete-unary-real cos "cos")
  (abstract-unary-real cos "cos")
  (symbolic-unary-real cos new-cos-unit "cos")
  (lambda (v) "cos")
  (if *imprecise-inexacts?*
      '(lambda ((forward x))
	(let ((x (primal (forward x)))
	      ((perturbation x) (tangent (forward x))))
	 (bundle
	  (cos x) (perturb (- 0.0 (* (sin x) (unperturb (perturbation x))))))))
      '(lambda ((forward x))
	(let ((x (primal (forward x)))
	      ((perturbation x) (tangent (forward x))))
	 (bundle (cos x)
		 (perturb (- 0 (* (sin x) (unperturb (perturbation x)))))))))
  (if *imprecise-inexacts?*
      '(lambda ((reverse x))
	(let ((x (*j-inverse (reverse x))))
	 (cons
	  (*j (cos x))
	  (lambda ((sensitivity y))
	   (sensitize
	    (cons cos (- 0.0 (* (sin x) (unsensitize (sensitivity y))))))))))
      '(lambda ((reverse x))
	(let ((x (*j-inverse (reverse x))))
	 (cons
	  (*j (cos x))
	  (lambda ((sensitivity y))
	   (sensitize
	    (cons cos (- 0 (* (sin x) (unsensitize (sensitivity y))))))))))))
 (define-primitive-procedure 'atan
  (concrete-binary-real atan "atan")
  (abstract-binary-real atan "atan")
  (symbolic-binary-real atan new-atan-unit "atan")
  (lambda (v) (c:builtin-name "atantwo" v))
  '(lambda ((forward x))
    (let (((cons x1 x2) (primal (forward x)))
	  ((cons x1-unperturbed x2-unperturbed)
	   (unperturb (tangent (forward x)))))
     (bundle (atan x2 x1)
	     (perturb (/ (- (* x1 x2-unperturbed) (* x2 x1-unperturbed))
			 (+ (* x1 x1) (* x2 x2)))))))
  (if *imprecise-inexacts?*
      '(lambda ((reverse x))
	(let (((cons x1 x2) (*j-inverse (reverse x))))
	 (cons (*j (atan x2 x1))
	       (lambda ((sensitivity y))
		(sensitize
		 (cons atan
		       (cons (- 0.0
				(/ (* x2 (unsensitize (sensitivity y)))
				   (+ (* x1 x1) (* x2 x2))))
			     (/ (* x1 (unsensitize (sensitivity y)))
				(+ (* x1 x1) (* x2 x2))))))))))
      '(lambda ((reverse x))
	(let (((cons x1 x2) (*j-inverse (reverse x))))
	 (cons (*j (atan x2 x1))
	       (lambda ((sensitivity y))
		(sensitize
		 (cons atan
		       (cons (- 0
				(/ (* x2 (unsensitize (sensitivity y)))
				   (+ (* x1 x1) (* x2 x2))))
			     (/ (* x1 (unsensitize (sensitivity y)))
				(+ (* x1 x1) (* x2 x2))))))))))))
 (define-primitive-procedure '=
  (concrete-binary-real-predicate = "=")
  (abstract-binary-real-predicate = "=")
  (symbolic-binary-real-predicate = new-=-unit "=")
  (lambda (v) (c:builtin-name "eq" v))
  '(lambda ((forward x))
    (let (((cons x1 x2) (primal (forward x)))
	  (j* (lambda (x) (bundle x (zero (perturb x))))))
     (j* (= x1 x2))))
  '(lambda ((reverse x))
    (let (((cons x1 x2) (*j-inverse (reverse x))))
     (cons (*j (= x1 x2))
	   (lambda ((sensitivity y))
	    (sensitize (cons = (cons (zero x1) (zero x2)))))))))
 (define-primitive-procedure '<
  (concrete-binary-real-predicate < "<")
  (abstract-binary-real-predicate < "<")
  (symbolic-binary-real-predicate < new-<-unit "<")
  (lambda (v) (c:builtin-name "lt" v))
  '(lambda ((forward x))
    (let (((cons x1 x2) (primal (forward x)))
	  (j* (lambda (x) (bundle x (zero (perturb x))))))
     (j* (< x1 x2))))
  '(lambda ((reverse x))
    (let (((cons x1 x2) (*j-inverse (reverse x))))
     (cons (*j (< x1 x2))
	   (lambda ((sensitivity y))
	    (sensitize (cons < (cons (zero x1) (zero x2)))))))))
 (define-primitive-procedure '>
  (concrete-binary-real-predicate > ">")
  (abstract-binary-real-predicate > ">")
  (symbolic-binary-real-predicate > new->-unit ">")
  (lambda (v) (c:builtin-name "gt" v))
  '(lambda ((forward x))
    (let (((cons x1 x2) (primal (forward x)))
	  (j* (lambda (x) (bundle x (zero (perturb x))))))
     (j* (> x1 x2))))
  '(lambda ((reverse x))
    (let (((cons x1 x2) (*j-inverse (reverse x))))
     (cons (*j (> x1 x2))
	   (lambda ((sensitivity y))
	    (sensitize (cons > (cons (zero x1) (zero x2)))))))))
 (define-primitive-procedure '<=
  (concrete-binary-real-predicate <= "<=")
  (abstract-binary-real-predicate <= "<=")
  (symbolic-binary-real-predicate <= new-<=-unit "<=")
  (lambda (v) (c:builtin-name "le" v))
  '(lambda ((forward x))
    (let (((cons x1 x2) (primal (forward x)))
	  (j* (lambda (x) (bundle x (zero (perturb x))))))
     (j* (<= x1 x2))))
  '(lambda ((reverse x))
    (let (((cons x1 x2) (*j-inverse (reverse x))))
     (cons (*j (<= x1 x2))
	   (lambda ((sensitivity y))
	    (sensitize (cons <= (cons (zero x1) (zero x2)))))))))
 (define-primitive-procedure '>=
  (concrete-binary-real-predicate >= ">=")
  (abstract-binary-real-predicate >= ">=")
  (symbolic-binary-real-predicate >= new->=-unit ">=")
  (lambda (v) (c:builtin-name "ge" v))
  '(lambda ((forward x))
    (let (((cons x1 x2) (primal (forward x)))
	  (j* (lambda (x) (bundle x (zero (perturb x))))))
     (j* (>= x1 x2))))
  '(lambda ((reverse x))
    (let (((cons x1 x2) (*j-inverse (reverse x))))
     (cons (*j (>= x1 x2))
	   (lambda ((sensitivity y))
	    (sensitize (cons >= (cons (zero x1) (zero x2)))))))))
 (define-primitive-procedure 'zero?
  (concrete-unary-real-predicate zero? "zero?")
  (abstract-unary-real-predicate zero? "zero?")
  (symbolic-unary-real-predicate zero? new-zero?-unit "zero?")
  (lambda (v) (c:builtin-name "iszero" v))
  '(lambda ((forward x))
    (let ((x (primal (forward x)))
	  (j* (lambda (x) (bundle x (zero (perturb x))))))
     (j* (zero? x))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons (*j (zero? x))
	   (lambda ((sensitivity y)) (sensitize (cons zero? (zero x))))))))
 (define-primitive-procedure 'positive?
  (concrete-unary-real-predicate positive? "positive?")
  (abstract-unary-real-predicate positive? "positive?")
  (symbolic-unary-real-predicate positive? new-positive?-unit "positive?")
  (lambda (v) (c:builtin-name "positive" v))
  '(lambda ((forward x))
    (let ((x (primal (forward x)))
	  (j* (lambda (x) (bundle x (zero (perturb x))))))
     (j* (positive? x))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons (*j (positive? x))
	   (lambda ((sensitivity y)) (sensitize (cons positive? (zero x))))))))
 (define-primitive-procedure 'negative?
  (concrete-unary-real-predicate negative? "negative?")
  (abstract-unary-real-predicate negative? "negative?")
  (symbolic-unary-real-predicate negative? new-negative?-unit "negative?")
  (lambda (v) (c:builtin-name "negative" v))
  '(lambda ((forward x))
    (let ((x (primal (forward x)))
	  (j* (lambda (x) (bundle x (zero (perturb x))))))
     (j* (negative? x))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons (*j (negative? x))
	   (lambda ((sensitivity y)) (sensitize (cons negative? (zero x))))))))
 (define-primitive-procedure 'null?
  (concrete-unary-predicate vlad-empty-list?)
  (abstract-unary-predicate vlad-empty-list?)
  (symbolic-unary-predicate vlad-empty-list?)
  (lambda (v) (c:builtin-name "null" v))
  '(lambda ((forward x))
    (let ((x (primal (forward x)))
	  (j* (lambda (x) (bundle x (zero (perturb x))))))
     (j* (null? x))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons (*j (null? x))
	   (lambda ((sensitivity y)) (sensitize (cons null? (zero x))))))))
 (define-primitive-procedure 'boolean?
  (concrete-unary-predicate vlad-boolean?)
  (abstract-unary-predicate vlad-boolean?)
  (symbolic-unary-predicate vlad-boolean?)
  (lambda (v) (c:builtin-name "boolean" v))
  '(lambda ((forward x))
    (let ((x (primal (forward x)))
	  (j* (lambda (x) (bundle x (zero (perturb x))))))
     (j* (boolean? x))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons (*j (boolean? x))
	   (lambda ((sensitivity y)) (sensitize (cons boolean? (zero x))))))))
 (define-primitive-procedure 'real?
  (concrete-unary-predicate vlad-real?)
  (abstract-unary-predicate vlad-real?)
  (symbolic-unary-predicate vlad-real?)
  (lambda (v) (c:builtin-name "is_real" v))
  '(lambda ((forward x))
    (let ((x (primal (forward x)))
	  (j* (lambda (x) (bundle x (zero (perturb x))))))
     (j* (real? x))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons (*j (real? x))
	   (lambda ((sensitivity y)) (sensitize (cons real? (zero x))))))))
 (define-primitive-procedure 'pair?
  (concrete-unary-predicate vlad-pair?)
  (abstract-unary-predicate vlad-pair?)
  (symbolic-unary-predicate vlad-pair?)
  (lambda (v) (c:builtin-name "pair" v))
  '(lambda ((forward x))
    (let ((x (primal (forward x)))
	  (j* (lambda (x) (bundle x (zero (perturb x))))))
     (j* (pair? x))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons (*j (pair? x))
	   (lambda ((sensitivity y)) (sensitize (cons pair? (zero x))))))))
 (define-primitive-procedure 'procedure?
  ;; needs work: This should probably return #f for any transformed procedure.
  (concrete-unary-predicate vlad-procedure?)
  (abstract-unary-predicate vlad-procedure?)
  (symbolic-unary-predicate vlad-procedure?)
  (lambda (v) (c:builtin-name "procedure" v))
  '(lambda ((forward x))
    (let ((x (primal (forward x)))
	  (j* (lambda (x) (bundle x (zero (perturb x))))))
     (j* (procedure? x))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons
      (*j (procedure? x))
      (lambda ((sensitivity y)) (sensitize (cons procedure? (zero x))))))))
 ;; The perturbation?, forward?, sensitivity? and reverse? primitives are not
 ;; referentially transparent and violate the forward-transformation rule for
 ;; functions that only rearrange data.
 (define-primitive-procedure 'perturbation?
  (concrete-unary-predicate perturbation-value?)
  (abstract-unary-predicate perturbation-value?)
  (symbolic-unary-predicate perturbation-value?)
  (lambda (v) (c:builtin-name "perturbation" v))
  '(lambda ((forward x))
    (let ((x (primal (forward x)))
	  (j* (lambda (x) (bundle x (zero (perturb x))))))
     (j* (perturbation? x))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons
      (*j (perturbation? x))
      (lambda ((sensitivity y)) (sensitize (cons perturbation? (zero x))))))))
 (define-primitive-procedure 'forward?
  (concrete-unary-predicate forward-value?)
  (abstract-unary-predicate forward-value?)
  (symbolic-unary-predicate forward-value?)
  (lambda (v) (c:builtin-name "forward" v))
  '(lambda ((forward x))
    (let ((x (primal (forward x)))
	  (j* (lambda (x) (bundle x (zero (perturb x))))))
     (j* (forward? x))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons (*j (forward? x))
	   (lambda ((sensitivity y)) (sensitize (cons forward? (zero x))))))))
 (define-primitive-procedure 'sensitivity?
  (concrete-unary-predicate sensitivity-value?)
  (abstract-unary-predicate sensitivity-value?)
  (symbolic-unary-predicate sensitivity-value?)
  (lambda (v) (c:builtin-name "sensitivity" v))
  '(lambda ((forward x))
    (let ((x (primal (forward x)))
	  (j* (lambda (x) (bundle x (zero (perturb x))))))
     (j* (sensitivity? x))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons
      (*j (sensitivity? x))
      (lambda ((sensitivity y)) (sensitize (cons sensitivity? (zero x))))))))
 (define-primitive-procedure 'reverse?
  (concrete-unary-predicate reverse-value?)
  (abstract-unary-predicate reverse-value?)
  (symbolic-unary-predicate reverse-value?)
  (lambda (v) (c:builtin-name "reverse" v))
  '(lambda ((forward x))
    (let ((x (primal (forward x)))
	  (j* (lambda (x) (bundle x (zero (perturb x))))))
     (j* (reverse? x))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons (*j (reverse? x))
	   (lambda ((sensitivity y)) (sensitize (cons reverse? (zero x))))))))
 (define-primitive-procedure 'if-procedure
  concrete-if-procedure
  abstract-if-procedure
  symbolic-if-procedure
  (lambda (v) (c:builtin-name "if_procedure" v))
  '(lambda ((forward x))
    (let (((cons* x1 x2 x3) (primal (forward x)))
	  ((cons* x1-unperturbed x2-unperturbed x3-unperturbed)
	   (unperturb (tangent (forward x))))
	  (j* (lambda (x) (bundle x (zero (perturb x))))))
     (if x1
	 ((bundle x2 (perturb x2-unperturbed)) (j* '()))
	 ((bundle x3 (perturb x3-unperturbed)) (j* '())))))
  '(lambda ((reverse x))
    (let (((cons* x1 x2 x3) (*j-inverse (reverse x))))
     (if x1
	 (let (((cons (reverse y) y-backpropagator) ((*j x2) (*j '()))))
	  (cons
	   (reverse y)
	   (lambda ((sensitivity y))
	    (sensitize
	     (cons
	      if-procedure
	      (cons*
	       (zero x1)
	       ;; (sensitize
	       ;;  (cdr (unsensitize (y-backpropagator (sensitivity y)))))
	       ;; should be the sensitivity to the ignored '() argument of x2
	       ((lambda ((cons x y)) x)
		(unsensitize (y-backpropagator (sensitivity y))))
	       (zero x3)))))))
	 (let (((cons (reverse y) y-backpropagator) ((*j x3) (*j '()))))
	  (cons
	   (reverse y)
	   (lambda ((sensitivity y))
	    (sensitize
	     (cons
	      if-procedure
	      (cons*
	       (zero x1)
	       (zero x2)
	       ;; (sensitize
	       ;;  (cdr (unsensitize (y-backpropagator (sensitivity y)))))
	       ;; should be the sensitivity to the ignored '() argument of x3
	       ((lambda ((cons x y)) x)
		(unsensitize (y-backpropagator (sensitivity y))))))))))))))
 (define-primitive-procedure 'read-real
  (concrete-unary concrete-read-real)
  (abstract-unary abstract-read-real)
  (symbolic-unary symbolic-read-real new-read-real-unit)
  (lambda (v) "read_real")
  (if *imprecise-inexacts?*
      `(lambda (',(j* (vlad-empty-list))) (bundle (read-real) (perturb 0.0)))
      `(lambda (',(j* (vlad-empty-list))) (bundle (read-real) (perturb 0))))
  `(lambda (',(*j (vlad-empty-list)))
    (cons (*j (read-real))
	  (lambda ((sensitivity y)) (sensitize (cons read-real '()))))))
 (define-primitive-procedure 'real
  (concrete-unary-real (lambda (v) v) "real")
  (abstract-unary-real (lambda (v) (abstract-real)) "real")
  (symbolic-unary-real (lambda (v) (abstract-real)) identity "real")
  (lambda (v) (c:builtin-name "real" v))
  ;; These widen the tangent and cotangent as well. Nothing requires us to do
  ;; so. It is just a design decision.
  '(lambda ((forward x))
    (let ((x (primal (forward x))) ((perturbation x) (tangent (forward x))))
     (bundle (real x) (perturb (real (unperturb (perturbation x)))))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons (*j (real x))
	   (lambda ((sensitivity y))
	    (sensitize (cons real (real (unsensitize (sensitivity y))))))))))
 (define-primitive-procedure 'write-real
  (concrete-unary-real
   (lambda (v)
    ((if *pp?* pp write) (externalize v))
    (newline)
    v)
   "write-real")
  (abstract-unary-real (lambda (v) v) "write-real")
  (symbolic-unary-real (lambda (v) v) new-write-real-unit "write-real")
  (lambda (v) (c:builtin-name "write_real" v))
  '(lambda ((forward x))
    (let ((x (primal (forward x))) ((perturbation x) (tangent (forward x))))
     ;; The unperturb composed with perturb could be optimized away.
     (bundle (write-real x) (perturb (unperturb (perturbation x))))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons (*j (write-real x))
	   (lambda ((sensitivity y))
	    (sensitize (cons write-real (unsensitize (sensitivity y)))))))))
 (define-primitive-procedure 'write
  (concrete-unary (lambda (v)
		   ((if *pp?* pp write) (externalize v))
		   (newline)
		   v))
  (abstract-unary (lambda (v) v))
  (symbolic-unary (lambda (v) v) (lambda (v) (unimplemented "write")))
  (lambda (v) (unimplemented "write"))
  '(lambda ((forward x))
    (let ((x (primal (forward x))) ((perturbation x) (tangent (forward x))))
     ;; The unperturb composed with perturb could be optimized away.
     (bundle (write x) (perturb (unperturb (perturbation x))))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons (*j (write x))
	   (lambda ((sensitivity y))
	    (sensitize (cons write (unsensitize (sensitivity y)))))))))
 (define-primitive-procedure 'zero
  (concrete-ad zero)
  (abstract-ad zero)
  (symbolic-ad zero)
  (lambda (v) (c:builtin-name "zero" v))
  '(lambda ((forward x))
    (let ((x (primal (forward x))) ((perturbation x) (tangent (forward x))))
     ;; The unperturb-perturb could be optimized away.
     (bundle (zero x) (perturb (zero (unperturb (perturbation x)))))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons (*j (zero x))
	   (lambda ((sensitivity y)) (sensitize (cons zero (zero x))))))))
 (define-primitive-procedure 'perturb
  (concrete-ad perturb)
  (abstract-ad perturb)
  (symbolic-ad perturb)
  (lambda (v) (c:builtin-name "perturb" v))
  '(lambda ((forward x))
    (let ((x (primal (forward x))) ((perturbation x) (tangent (forward x))))
     ;; The unperturb composed with perturb could be optimized away.
     (bundle (perturb x) (perturb (perturb (unperturb (perturbation x)))))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons
      (*j (perturb x))
      ;; The argument must be called y-perturbation so as not to confuse the
      ;; tags.
      (lambda ((sensitivity y-perturbation))
       (sensitize
	(cons perturb
	      (unperturb (unsensitize (sensitivity y-perturbation))))))))))
 (define-primitive-procedure 'unperturb
  (concrete-ad unperturb)
  (abstract-ad unperturb)
  (symbolic-ad unperturb)
  (lambda (v) (c:builtin-name "unperturb" v))
  ;; The argument must be called x-perturbation so as not to confuse the tags.
  '(lambda ((forward x-perturbation))
    (let ((x-perturbation (primal (forward x-perturbation)))
	  ((perturbation x-perturbation) (tangent (forward x-perturbation))))
     (bundle (unperturb x-perturbation)
	     ;; The unperturb composed with perturb could be optimized away.
	     (perturb (unperturb (unperturb (perturbation x-perturbation)))))))
  ;; The argument must be called x-perturbation so as not to confuse the tags.
  '(lambda ((reverse x-perturbation))
    (let ((x-perturbation (*j-inverse (reverse x-perturbation))))
     (cons (*j (unperturb x-perturbation))
	   (lambda ((sensitivity y))
	    (sensitize
	     (cons unperturb (perturb (unsensitize (sensitivity y))))))))))
 (define-primitive-procedure 'primal
  (concrete-ad primal)
  (abstract-ad primal)
  (symbolic-ad primal)
  (lambda (v) (c:builtin-name "primal" v))
  ;; The argument must be called x-forward so as not to confuse the tags.
  '(lambda ((forward x-forward))
    (let ((x-forward (primal (forward x-forward)))
	  ((perturbation x-forward) (tangent (forward x-forward))))
     (bundle (primal x-forward)
	     (perturb (primal (unperturb (perturbation x-forward)))))))
  ;; The argument must be called x-forward so as not to confuse the tags.
  '(lambda ((reverse x-forward))
    (let ((x-forward (*j-inverse (reverse x-forward))))
     (cons (*j (primal x-forward))
	   (lambda ((sensitivity y))
	    (sensitize
	     (cons primal
		   ;; needs work: I'm not sure that this is the inverse of
		   ;;             primal.
		   (bundle (unsensitize (sensitivity y))
			   (zero (tangent x-forward))))))))))
 (define-primitive-procedure 'tangent
  (concrete-ad tangent)
  (abstract-ad tangent)
  (symbolic-ad tangent)
  (lambda (v) (c:builtin-name "tangent" v))
  ;; The argument must be called x-forward so as not to confuse the tags.
  '(lambda ((forward x-forward))
    (let ((x-forward (primal (forward x-forward)))
	  ((perturbation x-forward) (tangent (forward x-forward))))
     (bundle (tangent x-forward)
	     (perturb (tangent (unperturb (perturbation x-forward)))))))
  ;; The argument must be called x-forward so as not to confuse the tags.
  '(lambda ((reverse x-forward))
    (let ((x-forward (*j-inverse (reverse x-forward))))
     (cons (*j (tangent x-forward))
	   ;; The argument must be called y-perturbation so as not to confuse
	   ;; the tags.
	   (lambda ((sensitivity y-perturbation))
	    (sensitize
	     (cons tangent
		   ;; needs work: I'm not sure that this is the inverse of
		   ;;             tangent.
		   (bundle (zero (primal x-forward))
			   (unsensitize (sensitivity y-perturbation))))))))))
 (define-primitive-procedure 'bundle
  (concrete-ad bundle)
  (abstract-ad bundle)
  (symbolic-ad bundle)
  (lambda (v) (c:builtin-name "bundle" v))
  '(lambda ((forward x))
    (let (((cons x1 (perturbation x2)) (primal (forward x)))
	  ((cons x1-unperturbed (perturbation x2-unperturbed))
	   (unperturb (tangent (forward x)))))
     (bundle
      ;; The unperturb composed with perturb could be optimized away.
      (bundle x1 (perturb (unperturb (perturbation x2))))
      (perturb
       (bundle x1-unperturbed
	       ;; The unperturb composed with perturb could be optimized away.
	       (perturb (unperturb (perturbation x2-unperturbed))))))))
  '(lambda ((reverse x))
    (let (((cons x1 (perturbation x2)) (*j-inverse (reverse x))))
     (cons
      (*j (bundle x1 (perturbation x2)))
      ;; The argument must be called y-forward so as not to confuse the tags.
      (lambda ((sensitivity y-forward))
       (sensitize
	(cons bundle
	      (cons (primal (unsensitize (sensitivity y-forward)))
		    (tangent (unsensitize (sensitivity y-forward)))))))))))
 (define-primitive-procedure 'sensitize
  (concrete-ad sensitize)
  (abstract-ad sensitize)
  (symbolic-ad sensitize)
  (lambda (v) (c:builtin-name "sensitize" v))
  '(lambda ((forward x))
    (let ((x (primal (forward x))) ((perturbation x) (tangent (forward x))))
     (bundle
      (sensitize x) (perturb (sensitize (unperturb (perturbation x)))))))
  '(lambda ((reverse x))
    (let ((x (*j-inverse (reverse x))))
     (cons
      (*j (sensitize x))
      ;; The argument must be called y-sensitivity so as not to confuse the
      ;; tags.
      (lambda ((sensitivity y-sensitivity))
       (sensitize
	(cons sensitize
	      (unsensitize (unsensitize (sensitivity y-sensitivity))))))))))
 (define-primitive-procedure 'unsensitize
  (concrete-ad unsensitize)
  (abstract-ad unsensitize)
  (symbolic-ad unsensitize)
  (lambda (v) (c:builtin-name "unsensitize" v))
  ;; The argument must be called x-sensitivity so as not to confuse the tags.
  '(lambda ((forward x-sensitivity))
    (let ((x-sensitivity (primal (forward x-sensitivity)))
	  ((perturbation x-sensitivity) (tangent (forward x-sensitivity))))
     (bundle
      (unsensitize x-sensitivity)
      (perturb (unsensitize (unperturb (perturbation x-sensitivity)))))))
  ;; The argument must be called x-sensitivity so as not to confuse the tags.
  '(lambda ((reverse x-sensitivity))
    (let ((x-sensitivity (*j-inverse (reverse x-sensitivity))))
     (cons
      (*j (unsensitize x-sensitivity))
      (lambda ((sensitivity y))
       (sensitize
	;; The unsensitize composed with sensitize could be optimized away.
	(cons unsensitize (sensitize (unsensitize (sensitivity y))))))))))
 (define-primitive-procedure 'plus
  (concrete-ad plus)
  (abstract-ad plus)
  (symbolic-ad plus)
  (lambda (v) (c:builtin-name "plus" v))
  '(lambda ((forward x))
    (let (((cons x1 x2) (primal (forward x)))
	  ((cons x1-unperturbed x2-unperturbed)
	   (unperturb (tangent (forward x)))))
     (bundle (plus x1 x2) (perturb (plus x1-unperturbed x2-unperturbed)))))
  '(lambda ((reverse x))
    (let (((cons x1 x2) (*j-inverse (reverse x))))
     (cons (*j (plus x1 x2))
	   (lambda ((sensitivity y))
	    (sensitize (cons plus
			     (cons (unsensitize (sensitivity y))
				   (unsensitize (sensitivity y))))))))))
 (define-primitive-procedure '*j
  (concrete-ad *j)
  (abstract-ad *j)
  (symbolic-ad *j)
  (lambda (v) (c:builtin-name "starj" v))
  '(lambda ((forward x))
    (let ((x (primal (forward x))) ((perturbation x) (tangent (forward x))))
     (bundle (*j x) (perturb (*j (unperturb (perturbation x)))))))
  '(lambda ((reverse x))
    ;; The *j-inverse composed with *j could be optimized away.
    (let ((x (*j-inverse (reverse x))))
     (cons
      (*j (*j x))
      ;; The argument must be called y-reverse so as not to confuse the tags.
      (lambda ((sensitivity y-reverse))
       (sensitize
	(cons *j (*j-inverse (unsensitize (sensitivity y-reverse))))))))))
 (define-primitive-procedure '*j-inverse
  (concrete-ad *j-inverse)
  (abstract-ad *j-inverse)
  (symbolic-ad *j-inverse)
  (lambda (v) (c:builtin-name "starj_inverse" v))
  ;; The argument must be called x-reverse so as not to confuse the tags.
  '(lambda ((forward x-reverse))
    (let ((x-reverse (primal (forward x-reverse)))
	  ((perturbation x-reverse) (tangent (forward x-reverse))))
     (bundle (*j-inverse x-reverse)
	     (perturb (*j-inverse (unperturb (perturbation x-reverse)))))))
  ;; The argument must be called x-reverse so as not to confuse the tags.
  '(lambda ((reverse x-reverse))
    (let ((x-reverse (*j-inverse (reverse x-reverse))))
     ;; The *j-inverse composed with *j could be optimized away.
     (cons
      (*j (*j-inverse x-reverse))
      (lambda ((sensitivity y))
       (sensitize (cons *j-inverse (*j (unsensitize (sensitivity y))))))))))
 (initialize-forwards-and-reverses!))

;;; Commands

;;; Tam V'Nishlam Shevah L'El Borei Olam

;;; Local Variables:
;;; lisp-body-indent: 1
;;; End:
