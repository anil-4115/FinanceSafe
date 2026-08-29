import { useEffect, useState } from 'react';
import { api } from '../services/api';

function EducationPage() {
  const [modules, setModules] = useState([]);
  const [literacy, setLiteracy] = useState(null);
  const [moduleDetail, setModuleDetail] = useState(null);
  const [quiz, setQuiz] = useState(null);
  const [result, setResult] = useState(null);
  const [answers, setAnswers] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api.get('/education').then(({ data }) => setModules(data)).catch(() => setError('Could not load lessons.'));
    api.get('/education/literacy').then(({ data }) => setLiteracy(data)).catch(() => { /* optional */ });
  }, []);

  async function openModule(moduleId) {
    setModuleDetail(null);
    setQuiz(null);
    setResult(null);
    setError('');
    try {
      const { data } = await api.get(`/education/${moduleId}`);
      setModuleDetail(data);
    } catch {
      setError('Could not open this lesson.');
    }
  }

  async function startQuiz() {
    setResult(null);
    try {
      const { data } = await api.get(`/education/${moduleDetail.id}/quiz`);
      setQuiz(data);
      setAnswers(new Array(data.length).fill(null));
    } catch {
      setError('This lesson has no quiz yet.');
    }
  }

  function answerAt(index, value) {
    setAnswers((current) => current.map((item, i) => (i === index ? Number(value) : item)));
  }

  async function submitQuiz() {
    setLoading(true);
    setError('');
    try {
      const { data } = await api.post(`/education/${moduleDetail.id}/attempt`, { answers });
      setResult(data);
      setQuiz(null);
      const fresh = await api.get('/education/literacy');
      setLiteracy(fresh.data);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not submit the quiz. Answer every question.');
    } finally {
      setLoading(false);
    }
  }

  function goBack() {
    setModuleDetail(null);
    setQuiz(null);
    setResult(null);
    setError('');
  }

  return (
    <div className="page-shell">
      <h2>Financial Education</h2>

      {literacy && (
        <section className="stats-grid three-wide">
          <div className="stat-card"><span>Literacy score</span><strong>{literacy.literacyScore} / 100</strong></div>
          <div className="stat-card"><span>Level</span><strong>{literacy.level}</strong></div>
          <div className="stat-card"><span>Quiz attempts</span><strong>{literacy.totalAttempts}</strong></div>
        </section>
      )}
      {literacy && <div className="panel info-box">{literacy.summary.map((line, index) => <p key={index}>{line}</p>)}</div>}

      {error && <p className="form-error" role="alert">{error}</p>}

      {!moduleDetail && (
        <section className="card-grid module-grid">
          {modules.map((module) => (
            <article className="module-card" key={module.id}>
              <span className="chip">{module.category}</span>
              <h3>{module.title}</h3>
              <p className="muted">{module.durationMins} min · {module.topic}</p>
              <button className="ghost-btn" onClick={() => openModule(module.id)}>{module.bestScorePct != null ? `Best score ${module.bestScorePct}% — revise` : 'Read and take quiz'}</button>
            </article>
          ))}
        </section>
      )}

      {moduleDetail && !quiz && !result && (
        <section className="panel lesson-panel">
          <div className="detail-header">
            <div>
              <p className="eyebrow">{moduleDetail.category} · {moduleDetail.durationMins} min</p>
              <h3>{moduleDetail.title}</h3>
            </div>
            <button className="ghost-btn" onClick={goBack}>Back to lessons</button>
          </div>
          <div className="lesson-content">{moduleDetail.content.split(/\n/).map((line, index) => line.trim() ? <p key={index}>{line}</p> : null)}</div>
          {moduleDetail.content.includes('quiz') || <button className="primary-btn" onClick={startQuiz}>Take the quiz</button>}
        </section>
      )}

      {moduleDetail && quiz && !result && (
        <section className="panel quiz-panel">
          <div className="panel-header"><h3>Quiz: {moduleDetail.title}</h3><span>{quiz.length} question(s)</span></div>
          {quiz.map((question, questionIndex) => (
            <div className="quiz-question" key={question.id}>
              <strong>{questionIndex + 1}. {question.question}</strong>
              <div className="quiz-options">
                {question.options.map((option, optionIndex) => (
                  <label key={`${question.id}-${optionIndex}`} className={`quiz-option ${answers[questionIndex] === optionIndex ? 'selected' : ''}`}>
                    <input type="radio" name={`q-${question.id}`} checked={answers[questionIndex] === optionIndex} onChange={() => answerAt(questionIndex, optionIndex)} />
                    <span>{option}</span>
                  </label>
                ))}
              </div>
            </div>
          ))}
          <button className="primary-btn" disabled={loading || answers.some((answer) => answer === null)} onClick={submitQuiz}>
            {loading ? 'Grading…' : 'Submit answers'}
          </button>
        </section>
      )}

      {moduleDetail && result && (
        <section className="panel result-panel">
          <div className="result-facts">
            <span className={`risk-badge level-${result.scorePct >= 70 ? 'safe' : result.scorePct >= 40 ? 'warning' : 'critical'}`}>{result.scorePct}%</span>
            <h3>You scored {result.correct}/{result.total}</h3>
            <p>Literacy score is now {result.literacyScore}.</p>
          </div>
          <div className="quiz-answers">
            {result.results.map((item) => (
              <div key={item.questionId} className={`answer-row ${item.correct ? 'correct' : 'wrong'}`}>
                <strong>{item.question}</strong>
                <p>Your answer: {item.yourAnswer}</p>
                <p>Correct answer: {item.correctAnswer}</p>
                <p className="muted">{item.explanation}</p>
              </div>
            ))}
          </div>
          <div className="step-actions">
            <button className="primary-btn" onClick={startQuiz}>Retake quiz</button>
            <button className="ghost-btn" onClick={goBack}>Back to lessons</button>
          </div>
        </section>
      )}
    </div>
  );
}

export default EducationPage;