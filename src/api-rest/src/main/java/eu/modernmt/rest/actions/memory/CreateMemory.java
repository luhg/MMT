package eu.modernmt.rest.actions.memory;

import eu.modernmt.facade.ModernMT;
import eu.modernmt.model.Memory;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.framework.routing.TemplateException;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = {"memories", "domains"}, method = HttpMethod.POST)
public class CreateMemory extends ObjectAction<Memory> {

    @Override
    protected Memory execute(RESTRequest req, Parameters _params) throws PersistenceException {
        Params params = (Params) _params;
        return ModernMT.memory.create(params.name);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException, TemplateException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        private final String name;

        public Params(RESTRequest req) throws ParameterParsingException, TemplateException {
            super(req);

            name = getString("name", false);
        }
    }

}
