(module saddle-FR-mzc mzscheme

(require "common-mzc.ss")

(define (run)
 (let* ((start (list 1.0 1.0))
	(f (lambda (x1 y1 x2 y2)
	    (d- (d+ (sqr x1) (sqr y1)) (d+ (sqr x2) (sqr y2)))))
	(x1*-y1*
	 (multivariate-argmin-F
	  (lambda (x1-y1)
	   (multivariate-max-R
	    (lambda (x2-y2)
	     (f (car x1-y1) (car (cdr x1-y1)) (car x2-y2) (car (cdr x2-y2))))
	    start))
	  start))
	(x1* (car x1*-y1*))
	(y1* (car (cdr x1*-y1*)))
	(x2*-y2*
	 (multivariate-argmax-R
	  (lambda (x2-y2) (f x1* y1* (car x2-y2) (car (cdr x2-y2)))) start))
	(x2* (car x2*-y2*))
	(y2* (car (cdr x2*-y2*))))
  (list (list (write-real x1*) (write-real y1*))
	(list (write-real x2*) (write-real y2*)))))

(run))
