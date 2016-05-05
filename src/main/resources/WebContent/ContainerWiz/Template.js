(function () {
	var boolCast = /^(?:1|true|yes|t|y)$/;
	ContainerWiz.SectionType = RSuite.CacheableObject
		.extend(Ember.ArrayProxy.PrototypeMixin, {
			content: []
		})
		.reopenClass({
			_requestSectionType: function (config, subPage) {
				return RSuite.services({
					service: "api/rsuite-container-wizard-plugin-ws-get-section-type-info",
					data: {
						confAlias: config,
						nextSubPageIdx: subPage
					}
				});
			},
			request: function (config) {
				var inst = this.getCached(config, false);
				var subPage;
				config = config.split(':');
				if (config.length > 1 && !isNaN(config[config.length - 1])) {
					subPage = config.pop();
					config = config.join(':');
				} else {
					subPage = 0;
					config = config[0];
				}
				var def = new $.Deferred();
				this._requestSectionType(config, subPage).done(function (content) {
					var oldContent = inst.get('content'),
						requirements = [ RSuite.success ];
					oldContent.replace(0, oldContent.length, content.map(function (item) {
						item.mayRepeat = boolCast.test(item.mayRepeat);
						item.isRequired = boolCast.test(item.isRequired);
						requirements.push(this._requestTemplates(item.xmlTemplateType).done(function (templates) {
							item.xmlTemplates = templates;
							item.xmlTemplates.forEach(function (tpl) {
								tpl.managedObject = RSuite.model.ManagedObject.get(tpl.moid);
								requirements.push(tpl.managedObject);
							});
						}).fail(def.reject.bind(def)));
						return item;
					}));
					RSuite.whenAll(requirements).done(function () {
						def.resolve({});
					});//Failures caught individually above
				}).fail(def.reject.bind(def));
				return def.promise();
			}
		});
}());
